package com.github.laboo.lwes.client;

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
 * run like this if you have gradle installed.
 *
 * > gradle runClient
 *
 * or
 *
 * > ./gradlew runClient
 *
 * if you don't.
 *
 */
/**
 * Created by mlibucha on 7/12/15.
 */
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.drafts.Draft_17;
import java.net.URI;
import javax.websocket.*;

public class WSClient extends WebSocketClient {

    public WSClient(URI uri, Draft d) {
        super(uri, d);
    }

    @OnError
    public void onError(Exception e) {
        System.out.println("got an error: " + e);
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received msg: " + message);
    }

    @OnOpen
    public void onOpen(ServerHandshake handshake) {
        send("{"
                + "'ip': '224.0.0.69',"
                + "'port': 9191,"
                + "'batchSize': 5,"
                + "'maxSecs': 60,"
                + "'requests': {'Click' : ['url','count'],"
                + "'Search' : ['term', 'lat', 'lon', 'count'],"
                + "'Ad' : ['text', 'count'] }"
                + "}");
    }

    @OnClose
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Closed");
    }

    public static void main(String[] args) {
        Draft d = new Draft_17();
        WSClient client = new WSClient(URI.create("ws://localhost:8887/"), d);
        client.connect();
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
        }
    }

}