NetAddr.langPort;
NetAddr.localAddr
thisProcess.openPorts;

n = NetAddr.new("192.168.56.1", 57120);    // create the NetAddr
// create the OSCFunc
o = OSCFunc({ arg msg, time, addr, recvPort; [msg, time, addr, recvPort].postln; }, '/goodbye', n);
o.free;    // remove the OSCFunc when you are done.

OSCFunc.trace(true); // Turn posting on
OSCFunc.trace(false); // Turn posting off