package com.github.laboo.lwes.websocketserver;

import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.lwes.emitter.MulticastEventEmitter;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Created by mlibucha on 6/11/15.
 *
 * These tests require that a server be running on localhost:8887
 */
public class TestIntegration {
    private static URI uri = URI.create("http://localhost:8887");
    private static Draft d = new Draft_17();
    private static Main main;


    @BeforeSuite
    public void setup() throws org.apache.commons.cli.ParseException {
        String[] args = new String[0];
        main = new Main(args);
        main.start();
    }

    @AfterSuite
    public void shutdown() {
        main.stop();
    }

    @Test(groups= {"integration"})
    public void testSimple() throws Exception {
        String config = "{'batchSize': 1, 'ip':'224.0.0.69','port':9191,'filters':[],'requests':{'MyEvent': ['key', 'z']}}";
        WSClient c = new WSClient(uri, d, config);

        c.connect();

        while (!c.getConnection().isOpen()) {
            Thread.sleep(1000);
        }

        org.lwes.Event e = emit("MyEvent", "key", 1);
        List<String> list = c.getAll(1000);

        if (list.isEmpty()) {
            throw new Exception("timed out");
        }

        c.close();
    }

    private <T> org.lwes.Event emit(String eventName, String attr, T value) throws IOException {
        MulticastEventEmitter emitter = new MulticastEventEmitter();
        emitter.setMulticastAddress(InetAddress.getByName("224.0.0.69"));
        emitter.setMulticastPort(9191);
        emitter.initialize();
        org.lwes.Event e = emitter.createEvent(eventName, false);
        if (value instanceof Integer) {
            e.setInt32(attr, (Integer) value);
        } else if (value instanceof String) {
            e.setString(attr, (String) value);
        } else {
            throw new IllegalArgumentException("Unrecognized type: "  + value.getClass());
        }
        emitter.emit(e);
        emitter.shutdown();
        return e;
    }

    private void emit (String eventName, Map<String,Object> values) throws IOException {
        MulticastEventEmitter emitter = new MulticastEventEmitter();
        emitter.setMulticastAddress(InetAddress.getByName("224.0.0.69"));
        emitter.setMulticastPort(9191);
        emitter.initialize();
        org.lwes.Event e = emitter.createEvent(eventName, false);
        for (Map.Entry<String,Object> entry : values.entrySet()) {
            String attr = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Integer) {
                e.setInt32(attr, (Integer) value);
            } else if (value instanceof String) {
                e.setString(attr, (String) value);
            } else {
                throw new IllegalArgumentException("Unrecognized type: "  + value.getClass());
            }
        }
        emitter.emit(e);
        emitter.shutdown();
    }
}
