package com.github.laboo.lwes.websocketserver;

import ch.qos.logback.classic.Logger;
import org.lwes.listener.DatagramEventListener;
import org.lwes.listener.EventHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import org.pcollections.*;

/**
 * Created by mlibucha on 5/10/15.
 */
public class Listener implements EventHandler{
    private static Logger log = Log.getLogger();
    public static int DEFAULT_QUEUE_SIZE = 5000;
    private String ip;
    private int port;
    private String channel;
    private int queueSize;
    DatagramEventListener listener;

    public Listener(String ip, int port) {
        this(ip, port, DEFAULT_QUEUE_SIZE);
    }

    public Listener(String ip, int port, int queueSize) {
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
        listener.initialize();
    }

    public void stop() {
        if (listener != null) {
            listener.shutdown();
        }
    }

    public void handleEvent(org.lwes.Event event) {
        handleEvent(event, channel);
    }

    // Static for simpler testing
    public static void handleEvent(org.lwes.Event event, String channel) {
        try {
            if (event != null) {
                PMap<String, PSet<ClientConfig>> requestMap = ConfigMap.getRequestMap(); // global map for all configs
                String name = event.getEventName();
                String key = channel + name;
                PSet<ClientConfig> configSet = requestMap.get(key); // Configs listening for this type of event
                PSet<ClientConfig> allConfigSet = requestMap.get(channel); // Configs listening on all events types

                if (configSet != null) {
                    handleConfigSet(configSet, null, event, name);
                }
                if (allConfigSet != null) {
                    handleConfigSet(allConfigSet, configSet, event, name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleConfigSet(PSet<ClientConfig> configSet,
                                        PSet<ClientConfig> excludedConfigSet,
                                        org.lwes.Event event,
                                        String name) {
        top: for (ClientConfig config : configSet) {
            if (excludedConfigSet != null && excludedConfigSet.contains(config)) {
                continue;
            }

            Event e = new Event(name); // This is the internal version of the event

            List<Filter> filters = config.getFilters();
            for (Filter filter : filters) {
                if (!filter.matches(event)) {
                    continue top; // One filter miss rejects the event for this config
                }
            }

            // Determine the list of attrs specified in the config, use "" as a shorthand for all event types
            List<String> configAttrsList = config.getRequests().get(name);
            if (configAttrsList == null) {
                // Try the empty string, which represents all types
                configAttrsList = config.getRequests().get("");
                if (configAttrsList == null) { // Should never happen
                    log.warn("Client config without proper requests, name=" + name +
                            " clientConfig=" + config);
                    return;
                }
            }

            Set<String> configAttrs = new HashSet<>(configAttrsList);
            boolean allAttributes = configAttrs.size() == 0;
            for (String attr : event.getEventAttributes()) {
                if (allAttributes || configAttrs.contains(attr)) {
                    e.setAttr(attr, event.get(attr));
                }
            }

            for (Client client : config.clients) {
                client.events.add(e);
            }
        }
    }

    public void destroy() {
        listener.removeHandler(this);
        listener.shutdown();
    }
}
