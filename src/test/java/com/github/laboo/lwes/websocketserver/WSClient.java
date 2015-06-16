package com.github.laboo.lwes.websocketserver;

/**
 * Created by mlibucha on 6/10/15.
 */

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.drafts.Draft_17;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.websocket.*;

public class WSClient extends  WebSocketClient {
    private String init;
    private BlockingQueue<String> q = new LinkedBlockingQueue<>();

    public WSClient(URI uri, Draft d, String init) {
        super(uri, d);
        this.init = init;
    }

    @OnError
    public void onError(Exception e) {

    }

    @OnMessage
    public void onMessage(String message) {
        q.add(message);
    }

    @OnOpen
    public void onOpen(ServerHandshake handshake) {
        this.send(this.init);
    }

    @OnClose
    public void onClose(int code, String reason, boolean remote) {

    }

    public void init() {
        this.connect();

    }

    public List<String> getAll(long ms) {
        long deadline = System.currentTimeMillis() + ms;
        List<String> list = new ArrayList<String>();
        while (deadline > System.currentTimeMillis()) {
            try {
                String str = q.poll(ms, TimeUnit.MILLISECONDS);
                if (str != null) {
                    list.add(str);
                }
            } catch (InterruptedException ie) {
                // ignore
            }
        }
        return list;
    }
}
