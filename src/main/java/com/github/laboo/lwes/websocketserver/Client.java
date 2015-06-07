package com.github.laboo.lwes.websocketserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
//import org.lwes.Event;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by mlibucha on 5/9/15.
 */
public class Client {

    private static ObjectMapper mapper = new ObjectMapper();
    private WebSocket conn;
    private ClientConfig config;
    public BlockingQueue<Event> events = new LinkedBlockingQueue<>();
    int batchSize = 1;
    int maxSecs = Integer.MAX_VALUE;
    private Thread t;

    private static class Sender implements Runnable {
        private BlockingQueue<Event> events = new LinkedBlockingQueue<>();
        private int batchSize;
        private int maxSecs;
        private WebSocket conn;
        private ClientConfig config;

        public Sender(BlockingQueue<Event> events,
                      int batchSize,
                      int maxSecs,
                      WebSocket conn,
                      ClientConfig config) {
            this.events = events;
            this.batchSize = batchSize;
            this.maxSecs = maxSecs;
            this.conn = conn;
            this.config = config;
        }

        public void run() {
            System.out.println("client running");
            while (true) {
                long maxMillis = maxSecs * 1000;
                long deadline = System.currentTimeMillis() + maxMillis;
                try {
                    List<Event> results = new ArrayList<>(batchSize);
                    while (results.size() < batchSize) {
                        long remaining = deadline - System.currentTimeMillis();
                        Event e = events.poll(remaining, TimeUnit.MILLISECONDS);
                        if (e == null) {
                            break;
                        }
                        results.add(e);
                    }
                    String data = mapper.writeValueAsString(new Response(results));
                    results.clear();
                    conn.send(data);
                } catch (Exception e) {
                    // All exceptions are fatal
                    if (!(e instanceof WebsocketNotConnectedException)) {
                        System.out.println("client thread closing conn: " + e);
                        this.conn.close();
                    }
                    return;
                }
            }
        }
    }

    public Client(WebSocket conn) {
        this.conn = conn;
    }

    public ClientConfig getConfig() {
        return config;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setMaxSecs(int maxSecs) { this.maxSecs = maxSecs; }

    public void setConfig(ClientConfig config) {
        this.config = config;
        this.t = new Thread(new Sender(this.events, this.batchSize, this.maxSecs, this.conn, this.config));
        this.t.start();
    }

    public WebSocket getConn() {
        return conn;
    }

    public void setConn(WebSocket conn) {
        this.conn = conn;
    }

    public void close() {
        this.t.interrupt();
    }
}
