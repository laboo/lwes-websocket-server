/**
 * Sends a request to the LWES WebSocket Server, which is listening on localhost:8887.
 * The config JSON object it sends asks the server to listen on 
 * (UDP Mulitcast) channel 224.0.0.69:9191, and forward all LWES events with names
 * 'Click', 'Search' or 'Ad' over the WebSocket with the LWES attributes listed.
 * The events are sent in batches of 5, or less if 60 seconds elapses before 5 are
 * found.
 *
 * If you start the server with the -e flag, like this:
 *
 * java -jar build/lib/lwes-websocket-server-1.0-all.jar -e 
 *
 * then the server emit the LWES events it needs to satisfy this client, which you
 * run like this:
 *
 * npm install websocket
 * node client.js
 */
var WebSocketClient = require('websocket').client;

var client = new WebSocketClient();

var config =
    {
	"ip": "224.0.0.69",
	"port": 9191,
	"batchSize": 5,
	"maxSecs": 60,
	"requests": {"Click" : ["url","count"],
		     "Search" : ["term", "lat", "lon","count"],
		     "Ad" : ["text", "count"] }
    }

client.on('connectFailed', function(error) {
    console.log('Connect Error: ' + error.toString());
});

client.on('connect', function(connection) {
    console.log('WebSocket Client Connected');
    connection.on('error', function(error) {
	console.log("Connection Error: " + error.toString());
    });
    connection.on('close', function() {
	console.log('Connection Closed');
    });
    connection.on('message', function(message) {
	if (message.type === 'utf8') {
	    console.log(message.utf8Data);
	}
    });
    connection.sendUTF(JSON.stringify(config));
});

client.connect('ws://127.0.0.1:8887/');

