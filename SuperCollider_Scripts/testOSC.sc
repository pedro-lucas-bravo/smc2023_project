NetAddr.langPort;
NetAddr.localAddr
thisProcess.openPorts;

thisProcess.openUDPPort(10000); //phone port
thisProcess.openUDPPort(10001); //mac port



n = NetAddr.new("192.168.43.129", 10000);    // create the NetAddr
(
var n1;
m = NetAddr.new("192.168.43.129", 10001);    // create the NetAddr
);
// create the OSCFunc
o = OSCFunc({ arg msg, time, addr, recvPort; [msg, time, addr, recvPort].postln; }, '/goodbye', n);
(
var o1;
p = OSCFunc({ arg msg, time, addr, recvPort; [msg, time, addr, recvPort].postln; }, '/goodbye', m);
)
o.free;    // remove the OSCFunc when you are done.
p.free;

OSCFunc.trace(true); // Turn posting on
OSCFunc.trace(false); // Turn posting off


// this example is basically like OSCFunc.trace but filters out
// /status.reply messages
(
f = { |msg, time, addr|
    if(msg[0] == '/oscControl/slider1') {
        //"time: % sender: %\nmessage: %\n".postf(time, addr, msg);
		//x.set(\freq, 200);
		var freq;
		freq = LinLin.kr(msg, 0.0, 1.0, 100, 200);
		"Freq %\n".postf(freq);
		x.set(\freq, freq);
    }
};
thisProcess.addOSCRecvFunc(f);
);

// stop posting.
thisProcess.removeOSCRecvFunc(f);


// The audio server is predefined in the variable s, so to boot it we can run the line:
s.boot;

// to make a sound we define a synth and add it to the server:
(
SynthDef(\sine, {|freq = 200, amp = 0.1|
	var sig;
	sig = Saw.ar(freq, amp);
	Out.ar(0, sig);
}).add;
)

// to play the synth we just defined we run
x = Synth(\sine);
x.set(\freq, 440);

x.free; // stop it

ServerOptions.devices;

///////////////////////////////////////////////////////////////////////////////////////

(
// make sure you have enough outputs
s.options.numOutputBusChannels = 32;
s.options.numInputBusChannels = 2;

// Instead of s.boot; we can use waitForBoot.
// This creates a Routine that allows for checking that certain server tasks have been completed before moving on.
// E.g. to make sure that soundfiles have been loaded into buffers before trying to play them.
s.waitForBoot{
	// A mono sound source to test with
	/*SynthDef(\dust, {|out = 2, gate = 1, density = 18, freq = 3600|
		var sig, dust, env;
		dust = Dust.kr(density);
		env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
		sig = BPF.ar(WhiteNoise.ar(0.2), freq, 0.3, Decay.kr(dust, TRand.kr(0.1, 0.5, dust)));
		sig = sig * env;
		sig = FoaEncode.ar(sig, FoaEncoderMatrix.newOmni); // encode the signal to an omnidirectional soundfield
		Out.ar(out, sig);
	}).add;*/

	SynthDef(\saw, {|out = 2, freq = 200, amp = 0.1|
		var sig;
		sig = Saw.ar(freq, amp);
		sig = FoaEncode.ar(sig, FoaEncoderMatrix.newOmni);
		Out.ar(out, sig);
	}).add;

	// Synth that transforms the FOA-encoded signal by pushing the soundfield image into certain directions. See FoaPush docs!
	SynthDef(\push, {|bus, angle = 0.5pi, azimuth, elevation, lagTime = 0.05|
		var sig;
		sig = In.ar(bus, 4);
		sig = FoaTransform.ar(sig, 'push', angle, azimuth.lag(lagTime), elevation.lag(lagTime)); // lag adds an interpolation to avoid clicks
		ReplaceOut.ar(bus, sig); // ReplaceOut overwrites the incoming signal (otherwise the transformed signal will be mixed with the source)
	}).add;

	// First Order Ambisonics Decoder Synth for Lilla Salen
	SynthDef(\LSfoaDecoder, {|foaInput|
		var foa;
		foa = In.ar(foaInput, 4);
		Out.ar(0, KMHLSDome1h1pN.ar(*foa));
		//Out.ar(0, FoaDecode.ar(
		// Note: the * before the foa array populates the decoder arguments one by one, using the array elements
		// It could be explicitly written as KMHLSDome1h1pN.ar(foa[0], foa[1], foa[2], foa[3]);
	}).add;

	// HRTF Decoder for Headphone monitoring
	~hrtf = FoaDecoderKernel.newListen;

	// UHJ Decoder for stereo monitoring
	~uhj = FoaDecoderKernel.newUHJ;

	// Note: The kernel decoders load impulse responses into buffers this needs to be handled outside of the SynthDef.
	// We have to make sure that the server has loaded these kernels before building the SynthDefs
	s.sync;

	SynthDef(\PHfoaDecoder, {|foaInput|
		var foa;
		foa = In.ar(foaInput, 4);
		foa = FoaDecode.ar(foa, ~hrtf);
		Out.ar(0, foa);
	}).add;

	// stereo decoder
	SynthDef(\UHJfoaDecoder, {|foaInput|
		var foa;
		foa = In.ar(foaInput, 4);
		foa = FoaDecode.ar(foa, ~uhj);
		Out.ar(0, foa);
	}).add;

	// Buses and Groups
	~foaBus = Bus.audio(s, 4); // an internal bus to use for the 4 channel Ambisonics B-format signal
	~sources = Group(s); // a group for sound sources
	~transforms = Group.after(~sources); // another group for transforms that placed after the sources

	s.sync;

	// start your preferred decoder synth and make sure it is placed after the transforms group
	//~decoder = Synth.after(~transforms, \LSfoaDecoder, [\foaInput, ~foaBus]);
	//~decoder = Synth.after(~transforms, \PHfoaDecoder, [\foaInput, ~foaBus]);
	~decoder = Synth.after(~transforms, \UHJfoaDecoder, [\foaInput, ~foaBus]);
};
)

// start the source synth. Set the output to the previously defined FOA bus and place it in the ~sources group.
x = Synth(\saw, [\out, ~foaBus], ~sources);

// start the push transform synth. Set the input to listen to
~xform = Synth(\push, [\bus, ~foaBus], ~transforms);
x.free;
s.meter;

// The push transform has three parameters:
// • angle: pi/2 = push to plane wave, 0 = omni directional
// • azimuth: pi --> -pi
// • elevation: -pi --> pi (effective range is 0 --> pi, since we don't have speakers below us here)
~xform.set(\angle, pi/2, \azimuth, -0.24, \elevation, 0);

// use FoaXformDisplay to learn more about what's going on!
f = FoaXformDisplay();

// control parameters using a Routine
(
r = Routine{
	inf.do{|i|
		~xform.set(
			\azimuth, (i%100).linlin(0, 99, 0.25pi, -0.25pi),
			\elevation, (i%500).linlin(0, 499, -pi, pi)
		);
		0.01.wait;
	};
}.play;
)
r.stop; x.free;

