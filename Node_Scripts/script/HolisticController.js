// var osc = require("osc");
// var socket;

class HolisticController extends EventTarget {

    constructor(canvas){
      super();

      this.canvas = canvas;
      this.variables = {};
      // this.osc_client = new node_osc.Client("10.10.40.152", 57121);
      // this.osc_client = new osc.WebSocketPort({
      //   url: "ws://localhost:4999", // URL to your Web Socket server.
      //   metadata: true
      // });
    
      // this.osc_client.open();
      console.log("=> Created osc lient");
      this.socket = io();
      this.clientToServer(
        {
          address: "/carrier/frequency",
          args: [
              {
                  type: "f",
                  value: 440
              }
          ]
        }
      );
    }

    setVariable(key, val){
      this.variables[key] = val;
      waxml.setVariable(key, val);
    }

    update(results){
  
      // store landmarks for current hand
        var points = {};
        ["leftHand", "rightHand", "face", "pose"].forEach(target => {
            let landmarks = results[`${target}Landmarks`];
            if(landmarks){
                landmarks.forEach((point, i) => {
                    Object.entries(point).forEach(([key, val]) => {
                        if(typeof val !== "undefined"){
                            points[target+i+key] = val;
                          }
                        else{
                          points[target+i+key] = 0.0;
                        }
                    });
                });
            }
            else {
              ["x", "y"].forEach(key => {
                [...Array(21).keys()].forEach(i => {
                  points[target+i+key] = 0.00001;
                })
              })
            }
        });
        // console.log("results", results);
        // console.log("points", points["leftHand0x"]);
        this.send_osc(points);
      
        this.dispatchEvent(new CustomEvent("update", {target: this}));
    }

    clientToServer(msg){
      this.socket.emit("clientToServer", msg);
    }

    send_osc(points){
      this.clientToServer({address: "/LeftX", args: [{type: "f", value: Math.max(0, points["leftHand9x"])}]});
      this.clientToServer({address: "/LeftY", args: [{type: "f", value: points["leftHand9y"]}]});
      this.clientToServer({address: "/RightX", args: [{type: "f", value: points["rightHand9x"]}]});
      this.clientToServer({address: "/RightY", args: [{type: "f", value: points["rightHand9y"]}]});
      this.clientToServer(
          {address: "/LeftOpen", args: [{type: "f", value: Math.max(0, -(points["leftHand12y"] - points["leftHand9y"]))}]}
      );
      this.clientToServer(
        {address: "/RightOpen", args: [{type: "f", value: Math.max(0, -(points["rightHand12y"] - points["rightHand9y"]))}]}
      );  
      this.clientToServer(
        {address: "/MouthOpen",args: [{type: "f", value: Math.max(0, -(points["face11y"] - points["face16y"]))}]}
      )
    }
  }