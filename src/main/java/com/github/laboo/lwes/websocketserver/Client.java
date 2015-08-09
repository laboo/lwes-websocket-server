package com.github.laboo.lwes.websocketserver;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by mlibucha on 5/9/15.
 */
public class Client {

    private static Logger log = Log.getLogger();
    private static ObjectMapper mapper = new ObjectMapper();
    private Session session;
    private ClientConfig config;
    public BlockingQueue<Event> events = new LinkedBlockingQueue<>();
    int batchSize = 1;
    int maxSecs = Integer.MAX_VALUE;
    private Thread t;

    private static class Sender implements Runnable {
        private BlockingQueue<Event> events = new LinkedBlockingQueue<>();
        private int batchSize;
        private int maxSecs;
        private Session session;
        private ClientConfig config;

        public Sender(BlockingQueue<Event> events,
                      int batchSize,
                      int maxSecs,
                      Session session,
                      ClientConfig config) {
            this.events = events;
            this.batchSize = batchSize;
            this.maxSecs = maxSecs;
            this.session = session;
            this.config = config;
        }

        public void run() {
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
                    session.getBasicRemote().sendText(data);
                } catch (Exception e) {
                    // All exceptions are fatal
                    if (!(e instanceof IllegalStateException)) {
                        // TODO is there a similar exception in Tyrus?
                        log.warn("client thread closing conn: " + e);
                        try {
                            this.session.close();
                        } catch (IOException ioe) {
                            log.warn("Exception closing session, " + ioe);
                        }
                    }
                    return;
                }
            }
        }
    }

    public Client(Session session) {
        this.session = session;
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
        this.t = new Thread(new Sender(this.events, this.batchSize, this.maxSecs, this.session, this.config));
        this.t.start();
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void close() {
        this.t.interrupt();
    }
}
