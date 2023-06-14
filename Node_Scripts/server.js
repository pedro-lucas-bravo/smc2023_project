
// HTTP Server

var app = require('express')();
var http = require('http').Server(app);
var osc = require("osc"),
    WebSocket = require("ws");
//var WebSocket = require("ws");

var io = require('socket.io')(http);



app.get('/*', function(req, res){
  var basePath = req.url;

  //Uncomment for param support

  var path = req.url;
  var pathArr = path.split("?");
  var basePath = pathArr[0];
  var restPath;
  if(pathArr.length>1){
  	 restPath = pathArr[1];
  	 //fix params here if necessary

  }



  res.sendFile(__dirname + basePath);
});


'use strict';

var os = require('os');
var ifaces = os.networkInterfaces();

Object.keys(ifaces).forEach(function (ifname) {
  var alias = 0;

  ifaces[ifname].forEach(function (iface) {
    if ('IPv4' !== iface.family || iface.internal !== false) {
      // skip over internal (i.e. 127.0.0.1) and non-ipv4 addresses
      return;
    }

    if (alias >= 1) {
      // this single interface has multiple ipv4 addresses
      console.log("http://" + alias, iface.address + ":3001");
    } else {
      // this interface has only one ipv4 adress
      console.log("http://" + iface.address + ":3001");
    }
    ++alias;
  });
});



http.listen(3001, function(){
  
  console.log('Server Running on localhost:3001');

});




// OSC Server
var osc = require("osc");

/*******************
 * OSC Over Serial *
 *******************/

// Instantiate a new OSC Serial Port.
// var serialPort = new osc.SerialPort({
//     devicePath: process.argv[2] || "/dev/tty.usbmodem221361"
// });

// serialPort.on("message", function (oscMessage) {
//     console.log(oscMessage);
// });

// // Open the port.
// serialPort.open();


/****************
 * OSC Over UDP *
 ****************/

var getIPAddresses = function () {
    var os = require("os"),
        interfaces = os.networkInterfaces(),
        ipAddresses = [];

    for (var deviceName in interfaces) {
        var addresses = interfaces[deviceName];
        for (var i = 0; i < addresses.length; i++) {
            var addressInfo = addresses[i];
            if (addressInfo.family === "IPv4" && !addressInfo.internal) {
                ipAddresses.push(addressInfo.address);
            }
        }
    }

    return ipAddresses;
};

var udpPort = new osc.UDPPort({
    localAddress: "0.0.0.0",
    localPort: 5001,
    remoteAddress: "192.168.43.129",
    remotePort: 10001
});

udpPort.on("ready", function () {
    var ipAddresses = getIPAddresses();

    console.log("Listening for OSC over UDP.");
    ipAddresses.forEach(function (address) {
        console.log(" Host:", address + ", Port:", udpPort.options.localPort);
    });
});

udpPort.on("message", oscMessage => {
    console.log(oscMessage);
    //socketPort.emit(oscMessage);
    io.emit('serverToClient', oscMessage);
    console.log("OSC message: ", oscMessage);
});

udpPort.on("error", function (err) {
    console.log(err);
});

udpPort.open();

io.on('connection', function(socket){
	console.log('client connected');
		
	socket.on('clientToServer', function(msg){
		
		// console.log('Got msg from server: ', msg);
    udpPort.send(msg)
    console.log("Sent message to server: ", msg);
	
	});
	
	
});


// // WebSocket
// var server = app.listen(4999);

// // Listen for Web Socket requests.
// var wss = new WebSocket.Server({
//   server: server
// });

// // Listen for Web Socket connections.
// wss.on("connection", function (socket) {
//   var socketPort = new osc.WebSocketPort({
//       socket: socket,
//       metadata: true
//   });

//   socketPort.on("message", function (oscMsg) {
//       console.log("An OSC Message was received!", oscMsg);
//   });
// });
