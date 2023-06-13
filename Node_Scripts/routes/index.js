var express = require('express');
var nodeOSC = require('node-osc');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Express' });
});

const client = new nodeOSC.Client('192.168.43.129', 10001);
client.send('/oscAddress', 200, () => {
  client.close();
});

module.exports = router;

