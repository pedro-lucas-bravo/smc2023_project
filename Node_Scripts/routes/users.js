var express = require('express');
var router = express.Router();
var nodeOSC = require('node-osc');

/* GET users listing. */
router.get('/', function(req, res, next) {
  res.sendFile('public/webaudio/index.html', {root: __dirname })
});

const client = new nodeOSC.Client('192.168.43.129', 10001);
client.send('/oscAddress', 200, () => {
  client.close();
});

module.exports = router;
