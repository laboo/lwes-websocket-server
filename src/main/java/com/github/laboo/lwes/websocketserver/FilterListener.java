package com.github.laboo.lwes.websocketserver;

import org.lwes.listener.DatagramEventListener;
import org.lwes.listener.EventHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mlibucha on 5/10/15.
 */
public class FilterListener implements EventHandler{
    public static int DEFAULT_QUEUE_SIZE = 5000;
    private String ip;
    private int port;
    private String channel;
    private int queueSize;
    DatagramEventListener listener;

    public FilterListener(String ip, int port) {
        this(ip, port, DEFAULT_QUEUE_SIZE);
    }

    public FilterListener(String ip, int port, int queueSize) {
        this.ip = ip;
        this.port = port;
        this.queueSize = queueSize;
        this.channel = ip + ":" + port;
    }

    public void start() throws UnknownHostException {
        if (listener == null) {
            listener = new DatagramEventListener();
            listener.setAddress(InetAddress.getByName(this.ip));
            listener.setPort(this.port);
            listener.addHandler(this);
            listener.setQueueSize(this.queueSize);
        }
        System.out.println("starting listener: " + this.ip + ":" + this.port);
        listener.initialize();
    }

    public void stop() {
        if (listener != null) {
            listener.shutdown();
        }
    }

    public void handleEvent(org.lwes.Event event) {

        if (event != null) {
            Map<String, Set<ClientConfig>> requestMap = ConfigMap.getRequestMap();
            String name = event.getEventName();
            String key = channel + name;

            Set<ClientConfig> configSet = requestMap.get(key);
            if (configSet == null) {
                return; // Nothing waiting on this event name
            }

            boolean allAttributes = false; // Is there a client that wants *all* the attributes

            top: for (ClientConfig config : configSet) {
                Map<String, Set<ClientConfig>> attrMap = new HashMap<>();
                Map<ClientConfig, Event> outMap = new HashMap<>();
                Filter[] filters = config.getFilters();
                for (Filter filter : filters) {
                    if (!filter.matches(event)) {
                        continue top; // One filter miss rejects the event for this config
                    }
                }

                String[] attrs = config.getRequests().get(name);

                if (attrs == null || attrs.length == 0) {
                    allAttributes = true; // Not specifying any attrs at all means you want them all
                    // The empty string represents 'all attrs'
                    if (attrMap.get("") == null) {
                        attrMap.put("", new HashSet<ClientConfig>());
                    }

                    attrMap.get("").add(config);
                } else {
                    // There's just a limited set of attrs
                    for (String attr : attrs) {
                        if (attrMap.get(attr) == null) {
                            attrMap.put(attr, new HashSet<ClientConfig>());
                        }
                        attrMap.get(attr).add(config);
                    }
                }

                if (allAttributes) {
                    for (String attr : event.getEventAttributes()) {
                        Object value = event.get(attr);
                        for (ClientConfig allAttrsConfig : requestMap.get("")) {
                            addAttr(outMap, allAttrsConfig, name, attr, value);
                            for (ClientConfig someAttrsConfig : requestMap.get(key)) {
                                addAttr(outMap, someAttrsConfig, name, attr, value);
                            }
                        }
                    }
                } else {
                    for (Map.Entry<String, Set<ClientConfig>> entry : attrMap.entrySet()) {
                        String attr = entry.getKey();
                        if (!event.isSet(attr)) {
                            continue;
                        }
                        Object value = event.get(attr);
                        for (ClientConfig someAttrsConfig : entry.getValue()) {
                            addAttr(outMap, someAttrsConfig, name, attr, value);
                        }
                    }
                }
                // Put each event on the queue for each client ties to the client config
                for (Map.Entry<ClientConfig,Event> entry : outMap.entrySet()) {
                    for (Client client : entry.getKey().clients) {
                        client.events.add(entry.getValue());
                    }
                }
            }
        }
    }

    private static void addAttr(Map<ClientConfig, Event> map,
                                ClientConfig config,
                                String name,
                                String attr,
                                Object value) {
        Event event = map.get(config);
        if (event == null) {
            event = new Event(name);
        }
        event.setAttr(attr, value);
        map.put(config,event);
    }

    public void destroy() {
        System.out.println("destroying listener: " + this.ip + ":" + this.port);
        listener.shutdown();
    }
}
