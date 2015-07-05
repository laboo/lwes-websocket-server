### LWEB WebSocket Server

A server that listens on UDP mulitcast channels (224.0.0.x:port) and serves up any [LWES](https://github.com/lwes/) events emitted on those channels to clients as JSON objects over [WebSockets](https://tools.ietf.org/html/rfc6455).

LWES events are simple messages consisting of a name and a map of attributes/value pairs. They are typically used as an alternative to log files for tracking events.

For example, let's say you have web site and want to track each search, click and ad. As these events happen, instead of writing each out to a log file, which is slow, you broadcast the events on the network. Then some single-purpose servers listen on the network, capture those events and write them out to log files.

What this project does is listen for those same events on the network, but instead of writing them out to disk, it serves them up over web sockets, through filters if desired.

### Building
Linux/Mac
```
./gradlew shadowJar
```
Windows
```
gradle.bat shadowJar
```

### Running (assumes you have JRE installed)

```
java -jar build/lib/lwes-websocket-server-1.0-all.jar -e
```
The ```-e``` flag emits events, 1 per second, for testing purposes. In production, ```-e``` should no be used.

By default, the server listens on port 8887. Change the port with a -p parameter. For example.

```
java -jar build/lib/lwes-websocket-server-1.0-all.jar -e -p 8888
```

### Connecting with a WebSocket client

The WebSocket client (full clients: [Java](http://localhost:8080/) and [JavaScript](http://localhost:8080/)) must pass a JSON object like the follow (see wiki for exact spec):

```
{
  "ip": "224.0.0.69",
  "port": 9191,
  "batchSize": 5,
  "maxSecs": 60,
  "requests": {"Click" : ["url","count"],
               "Search" : ["term", "lat", "lon","count"],
               "Ad" : ["text", "count"] }
}
```

This request asks the server to listen for LWES events on channel 224:0.0.69:9191 (a UDP Multicast port) for events with the names "Click" and "Search". If it finds them it sends them over the WebSocket including only the attributes specified for each name. So, for "Search" events, it returns the "term", "lat" and "lon" attributes and their values, but no others. It will send the events over the WebSocket in batches of 5 or every 60 seconds, whichever comes first.

Regular expressions and filters can be used to define which events and attributes should be returned. See the wiki for the details.

### The Response

The response, which comes in batches when the ```batchSize``` or ```maxSecs``` is reached, looks like this (for example):
```
{
  "type":"data","msg":"",
  "data":[{"name":"Search","attrs":{"count":429,"lon":-0.127625,"term":"the thing I'm searching for","lat":51.503363}},
          {"name":"Click","attrs":{"count":430,"url":"http://www.example.com"}},
          {"name":"Ad","attrs":{"count":431,"text":"Buy my product"}},
          {"name":"Click","attrs":{"count":432,"url":"http://www.example.com"}},
          {"name":"Ad","attrs":{"count":433,"text":"Buy my product"}}
	 ]
}
```
The only type other than ```data```, is ```error``` and you only see that when the sent JSON object initializing the connection is malformed.

### Errors

Here, we don't specify the port in the JSON object we send:

```
{
  "ip": "224.0.0.69",
  "port": 9191,
  "batchSize": 5,
  "maxSecs": 60,
  "requests": {"Click" : ["url","count"],
               "Search" : ["term", "lat", "lon","count"],
               "Ad" : ["text", "count"] }
}
```
Here's the error response we get:
```
{
  "type":"error",
   "msg":"port must be specified in {\"ip\":\"224.0.0.69\",\"batchSize\":5,\"maxSecs\":60,\"requests\":{\"Click\":[\"url\",\"count\"],\"Search\":[\"term\",\"lat\",\"lon\",\"count\"],\"Ad\":[\"text\",\"count\"]}}",
   "data":[]
}
```





