
/////////////////////////// Ambisonics

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

	SynthDef(\pulse, {|out = 2, bpm = 60, freq = 440|
		var sig;
		sig = Decay.kr(Metro.kr(bpm,Dseq([1, 0.25, 0.5, 0.25], inf))) * SinOsc.ar(freq, 0, 0.1);
		sig = sig + FreeVerb.ar(
            sig,
			1, //mix 0-1
            0.7, // room 0-1
            0.2 // damp 0-1 duh
        );
		sig = FoaEncode.ar(sig, FoaEncoderMatrix.newOmni);
		Out.ar(out, sig);
	}).add;

	// Synth that transforms the FOA-encoded signal by pushing the soundfield image into certain directions. See FoaPush docs!
	SynthDef(\pushx, {|bus, angle = 0, mul = 1, add = 0, lagTime = 0.05|
		var sig;
		sig = In.ar(bus, 4);
		sig = FoaTransform.ar(sig, 'pushX', angle.lag(lagTime), mul.lag(lagTime), add.lag(lagTime)); // lag adds an interpolation to avoid clicks
		ReplaceOut.ar(bus, sig); // ReplaceOut overwrites the incoming signal (otherwise the transformed signal will be mixed with the source)
	}).add;

	// Synth that transforms the FOA-encoded signal by pushing the soundfield image into certain directions. See FoaPush docs!
	SynthDef(\pushy, {|bus, angle = 0, mul = 1, add = 0, lagTime = 0.05|
		var sig;
		sig = In.ar(bus, 4);
		sig = FoaTransform.ar(sig, 'pushY', angle.lag(lagTime), mul.lag(lagTime), add.lag(lagTime)); // lag adds an interpolation to avoid clicks
		ReplaceOut.ar(bus, sig); // ReplaceOut overwrites the incoming signal (otherwise the transformed signal will be mixed with the source)
	}).add;

	// Synth that transforms the FOA-encoded signal by pushing the soundfield image into certain directions. See FoaPush docs!
	SynthDef(\pushz, {|bus, angle = 0, mul = 1, add = 0, lagTime = 0.05|
		var sig;
		sig = In.ar(bus, 4);
		sig = FoaTransform.ar(sig, 'pushZ', angle.lag(lagTime), mul.lag(lagTime), add.lag(lagTime)); // lag adds an interpolation to avoid clicks
		ReplaceOut.ar(bus, sig); // ReplaceOut overwrites the incoming signal (otherwise the transformed signal will be mixed with the source)
	}).add;

	// First Order Ambisonics Decoder Synth for Lilla Salen
	SynthDef(\LSfoaDecoder, {|foaInput|
		var foa;
		foa = In.ar(foaInput, 4);
		//Out.ar(0, KMHLSDome1h1pN.ar(*foa));
		Out.ar(0, FoaDecode.ar(foa, FoaDecoderMatrix.kmhLillaSalen));
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



	//////////////////////////////////////////////////////////////////////////// 2nd source

	// This time we use a stereo soundfile summed to mono


	b = Buffer.read(s, "C:/Users/Pedro/Documents/SMC2023/loopSMC2023.wav");

	// a synthdef with possibilities to play a soundfile from a certain position and
	// an embedded FoaRTT transformer (rotate, tilt and tumble)
	SynthDef(\soundfile, {|out, buf, rate = 1, gate = 1, gain = 1, pos, rotate, tilt, tumble|
		var sig, env;
		env = EnvGen.kr(Env.asr(0.02, 1, 0.9), gate, doneAction: 2);
		sig = PlayBuf.ar(2, buf, BufRateScale.kr(buf) * rate, gate, pos * BufFrames.kr(buf), 1);
		// sum to mono and apply envelope
		sig = sig.sum * env * gain;
		// encode the signal using newDirection set to the default values
		sig = FoaEncode.ar(sig, FoaEncoderMatrix.newDirection(0, 0));
		sig = FoaTransform.ar(sig, 'rtt', rotate.lag(0.02), tilt.lag(0.02), tumble.lag(0.02));
		Out.ar(out, sig);
	}).add;

	// First Order Ambisonics Decoder Synth for Lilla Salen
	SynthDef(\LSfoaDecoder2, {|foaInput|
		var foa;
		foa = In.ar(foaInput, 4);
		//Out.ar(0, KMHLSDome1h1pN.ar(*foa));
		Out.ar(0, FoaDecode.ar(foa, FoaDecoderMatrix.kmhLillaSalen));
	}).add;

	// HRTF Decoder for Headphone monitoring
	~hrtf2 = FoaDecoderKernel.newListen;

	// UHJ Decoder for stereo monitoring
	~uhj2 = FoaDecoderKernel.newUHJ;

	s.sync;

	SynthDef(\PHfoaDecoder2, {|foaInput|
		var foa;
		foa = In.ar(foaInput, 4);
		foa = FoaDecode.ar(foa, ~hrtf2);
		Out.ar(0, foa);
	}).add;

	// stereo decoder
	SynthDef(\UHJfoaDecoder2, {|foaInput|
		var foa;
		foa = In.ar(foaInput, 4);
		foa = FoaDecode.ar(foa, ~uhj2);
		Out.ar(0, foa);
	}).add;

	//~sources2 = Group(s);
	//~foaBus2 = Bus.audio(s, 4); // an internal bus to use for the 4 channel Ambisonics B-format signal
	//~transforms2 = Group.after(~sources2); // another group for transforms that placed after the sources

	s.sync;

	// start your preferred decoder synth and make sure it is placed after the transforms group
	//~decoder2 = Synth.after(~sources2, \LSfoaDecoder2, [\foaInput, ~foaBus2]);
	//~decoder2 = Synth.after(~sources2, \PHfoaDecoder2, [\foaInput, ~foaBus2]);
	//~decoder = Synth.after(~sources2, \UHJfoaDecoder2, [\foaInput, ~foaBus2]);

	// start your preferred decoder synth and make sure it is placed after the transforms group
	//~decoder = Synth.after(~transforms, \LSfoaDecoder, [\foaInput, ~foaBus]);
	//~decoder = Synth.after(~transforms, \PHfoaDecoder, [\foaInput, ~foaBus]);
	//~decoder = Synth.after(~transforms, \UHJfoaDecoder, [\foaInput, ~foaBus]);

	// Buses and Groups
	~foaBus = Bus.audio(s, 4); // an internal bus to use for the 4 channel Ambisonics B-format signal
	~sources = Group(s); // a group for sound sources
	~transforms = Group.after(~sources); // another group for transforms that placed after the sources

	s.sync;

	// start your preferred decoder synth and make sure it is placed after the transforms group
	~decoder = Synth.after(~transforms, \LSfoaDecoder, [\foaInput, ~foaBus]);
	//~decoder = Synth.after(~transforms, \PHfoaDecoder, [\foaInput, ~foaBus]);
	//~decoder = Synth.after(~transforms, \UHJfoaDecoder, [\foaInput, ~foaBus]);
};
)


(
// start the source synth. Set the output to the previously defined FOA bus and place it in the ~sources group.
x = Synth(\pulse, [\out, ~foaBus], ~sources);

// start the push transform synth. Set the input to listen to
(
~xform_x = Synth(\pushx, [\bus, ~foaBus], ~transforms);
~xform_y = Synth(\pushy, [\bus, ~foaBus], ~transforms);
~xform_z = Synth(\pushz, [\bus, ~foaBus], ~transforms);
)
)

x.free;

(
// start the source synth. Set the output to the previously defined FOA bus and place it in the ~sources group.
y = Synth(\soundfile, [\buf, b, \out, ~foaBus, \pos, 0.05]);
y.free;

/////////////////////////////////// OSC COmmunication
(

thisProcess.openUDPPort(10000); //phone port
thisProcess.openUDPPort(10001); //mac port

n = NetAddr.new("192.168.43.129", 10000);
m = NetAddr.new("192.168.43.129", 10001);

o = OSCFunc({ arg msg, time, addr, recvPort; [msg, time, addr, recvPort].postln; }, '/goodbye', n);
p = OSCFunc({ arg msg, time, addr, recvPort; [msg, time, addr, recvPort].postln; }, '/goodbye', m);

)

(
o.free;    // remove the OSCFunc when you are done.
p.free;
)


OSCFunc.trace(true); // Turn posting on
OSCFunc.trace(false); // Turn posting off

////// Coordinates Receiver

(
f = { |msg, time, addr|
    if(msg[0] == '/oscControl/slider2Dx') {
        //"time: % sender: %\nmessage: %\n".postf(time, addr, msg);
		~xform_y.set(\angle, LinLin.kr(msg, 0.0, 1.0, pi/2, -pi/2), \mul, 1, \add, 0);
		//~yform_y.set(\angle, LinLin.kr(msg, 0.0, 1.0, pi/2, -pi/2), \mul, 1, \add, 0);
    };
	 if(msg[0] == '/oscControl/slider2Dy') {
        //"time: % sender: %\nmessage: %\n".postf(time, addr, msg);
		~xform_x.set(\angle, LinLin.kr(msg, 0.0, 1.0, -pi/2, pi/2), \mul, 1, \add, 0);
		//~yform_x.set(\angle, LinLin.kr(msg, 0.0, 1.0, -pi/2, pi/2), \mul, 1, \add, 0);
    };
	if(msg[0] == '/oscControl/slider2') {
        //"time: % sender: %\nmessage: %\n".postf(time, addr, msg);
		~xform_z.set(\angle, LinLin.kr(msg, 0.0, 1.0, -pi/2, pi/2), \mul, 1, \add, 0);
    };
	if(msg[0] == '/oscControl/slider1') {
        //"time: % sender: %\nmessage: %\n".postf(time, addr, msg);
		var bpmParam;
		bpmParam = LinLin.kr(msg, 0.0, 1.0,80, 420);
		x.set(\bpm, bpmParam);
    };
	if(msg[0] == '/oscControl/slider3') {
        //"time: % sender: %\nmessage: %\n".postf(time, addr, msg);

	var freqParam;
		freqParam = LinLin.kr(msg, 0.0, 1.0,220, 1200);
		freqParam = freqParam * rrand(1, 5) ;
		x.set(\freq, freqParam);

		/*var scale;
		var index;
		scale = Scale.major;
		//"X %".postf(msg);
		index = LinLin.kr(msg, 0.0, 1.0,0, 6);

		x.set(\freq, scale.degreeToFreq(index, 60.midicps, 1));*/
    };
	if(msg[0] == '/RightOpen') {
		var rateParam;
        //"time: % sender: %\nmessage: %\n".postf(time, addr, msg);
		rateParam = LinLin.kr(msg, 0.0, 0.2,0.1, 1.0);
		y.set(\rate, rateParam);
    };
	if(msg[0] == '/RightX') {
        //"time: % sender: %\nmessage: %\n".postf(time, addr, msg);
		y.set(\rotate, LinLin.kr(msg, 0.0, 1.0, -1, 1));
    };
	if(msg[0] == '/RightY') {
        "time: % sender: %\nmessage: %\n".postf(time, addr, msg);
		y.set(\gain, LinLin.kr(msg, 0.0, 1.0, 1.0, 0.0));
    };

};
thisProcess.addOSCRecvFunc(f);
)

(
// stop posting.
thisProcess.removeOSCRecvFunc(f);
)







