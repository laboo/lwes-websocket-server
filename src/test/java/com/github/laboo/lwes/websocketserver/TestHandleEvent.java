package com.github.laboo.lwes.websocketserver;

import static org.testng.Assert.*;
import org.lwes.emitter.MulticastEventEmitter;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by mlibucha on 6/13/15.
 */
public class TestHandleEvent {

    static String IP = "1.2.3.4";
    static int PORT = 1234;
    static String CHANNEL = IP + ":" + PORT;

    @Test
    public static void simple() throws Exception {
        org.lwes.Event e = getLwesEvent("MyEvent");
        e.setInt64("key", 1);
        ClientConfig config = buildClientConfig("[]", "{'MyEvent':['key']}");
        Client client = new Client(null);
        config.addClient(client);
        ConfigMap.addClientConfig(config);
        Event event = getEvent(client, e, 10);
        assertTrue(event != null && event.equals(e));
        config.removeClient(client);
        ConfigMap.removeClientConfig(config);
    }

    @Test
    public static void simpleMultiple() throws Exception {
        ConfigMap.print();
        List<org.lwes.Event> es = new ArrayList<>();
        org.lwes.Event e = getLwesEvent("MyEvent");
        e.setInt64("key", 1);
        es.add(e);
        org.lwes.Event e2 = getLwesEvent("MyEvent");
        e2.setInt64("key", 42);
        es.add(e2);
        ClientConfig config = buildClientConfig("[]", "{'MyEvent':['key']}");
        Client client = new Client(null);
        config.addClient(client);
        ConfigMap.addClientConfig(config);
        List<Event> events = getEvents(client, es, 10);
        for (int i = 0; i < es.size(); i++) {
            assertTrue(events.get(i) != null && events.get(i).equals(es.get(i)));
        }
        config.removeClient(client);
        ConfigMap.removeClientConfig(config);
    }

    @Test
    public static void allMultiple() throws Exception {
        List<org.lwes.Event> es = new ArrayList<>();
        org.lwes.Event e = getLwesEvent("MyEvent");
        e.setInt64("key", 1);
        es.add(e);
        org.lwes.Event e2 = getLwesEvent("xxx"); // This event has no attributes but should be returned anyway
        es.add(e2);
        ClientConfig config = buildClientConfig("[]", "{'':['key']}"); // '' here means give me all events
        Client client = new Client(null);
        config.addClient(client);
        ConfigMap.addClientConfig(config);
        List<Event> events = getEvents(client, es, 10);
        for (int i = 0; i < es.size(); i++) {
            assertTrue(events.get(i) != null && events.get(i).equals(es.get(i)));
        }
        config.removeClient(client);
        ConfigMap.removeClientConfig(config);
    }

    public static org.lwes.Event getLwesEvent(String eventName) {
        MulticastEventEmitter emitter = new MulticastEventEmitter();
        return emitter.createEvent(eventName, false);
    }

    public static Event getEvent(Client client,
                                 org.lwes.Event e,
                                 int secs) throws Exception {
        List<org.lwes.Event> es = new ArrayList<>();
        es.add(e);
        List<Event> list = getEvents(client, es, secs);
        return list.get(0);
    }

    public static List<Event> getEvents(Client client,
                                        List<org.lwes.Event> es,
                                        int secs) throws Exception {
        long deadline = System.currentTimeMillis() + secs * 1000;
        List<Event> list = new ArrayList<>();
        for (org.lwes.Event e : es) {
            Listener.handleEvent(e, CHANNEL);
            long waitTime = deadline - System.currentTimeMillis();
            if (waitTime < 0) {
                break;
            }
            list.add(client.events.poll(waitTime, TimeUnit.MILLISECONDS));
        }
        return list;
    }

    public static ClientConfig buildClientConfig(String filters, String requests) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{'ip':'");
        sb.append(IP);
        sb.append("','port':");
        sb.append(PORT);
        sb.append(",'filters':");
        sb.append(filters);
        sb.append(",'requests':");
        sb.append(requests);
        sb.append("}");
        return ClientConfig.build(sb.toString());
    }
}
