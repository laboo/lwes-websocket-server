package com.github.laboo.lwes.websocketserver;

import ch.qos.logback.classic.Logger;
import org.pcollections.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mlibucha on 5/10/15.
 */
public enum ConfigMap {
    INSTANCE;

    private static Logger log = Log.getLogger();
    private static PMap<String, PSet<ClientConfig>> requestMap;

    static {
        requestMap = HashTreePMap.empty();
    }

    public static PMap<String, PSet<ClientConfig>> getRequestMap() {
        return requestMap;
    }

    public static synchronized void print() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n---------------------\n");
        for (Map.Entry<String, PSet<ClientConfig>> entry : requestMap.entrySet()) {
            sb.append(entry.getKey() + " => [");
            for (ClientConfig cc : entry.getValue()) {
                try {
                    sb.append("  \n");
                    sb.append("(" + cc.clients.size() + ") ");
                    sb.append(cc);
                } catch (Exception e) {
                    log.warn(e.toString());
                }
            }
            sb.append(" ]\n");
        }
        sb.append("---------------------");
        log.debug(sb.toString());
    }

    public static int sizeOfRequestMap() {
        return requestMap.size();
    }

    public static int numConfigs(String key) {
        PSet<ClientConfig> set = requestMap.get(key);
        return (set != null) ? set.size() : 0;
    }

    public static synchronized ClientConfig addClientConfig(ClientConfig clientConfig) {
        ClientConfig out = clientConfig;
        for (String name: clientConfig.getRequests().keySet()) {
            String requestKey = clientConfig.getChannel() + name;
            PSet<ClientConfig> existing = requestMap.get(requestKey);
            if (existing == null) {
                PSet<ClientConfig> newSet = HashTreePSet.empty();
                newSet = newSet.plus(clientConfig);
                requestMap = requestMap.plus(requestKey, newSet);
            } else {
                for (ClientConfig c : existing) {
                    if (c.equals(clientConfig)) {
                        // If we've already got an "equals" client config, we don't want to replace it
                        // with the new one. We want to leave the old one in place, and later the connection
                        // for the new client config will get added to the old client config.
                        out = c;
                        continue;
                    } else {
                        existing = existing.plus(clientConfig);
                        requestMap = requestMap.plus(requestKey, existing);
                    }
                }
            }
        }
        return out;
    }

    public synchronized static void removeClientConfig(ClientConfig clientConfig) {
        if (clientConfig.clients.size() != 0) {
            return;
        }
        for (Map.Entry<String,PSet<ClientConfig>> entry : requestMap.entrySet()) {
            PSet<ClientConfig> set = entry.getValue();
            for (ClientConfig cc : set) {
                if (cc.equals(clientConfig)) {
                    set = set.minus(cc);
                }
            }
            if (set.isEmpty()) {
                requestMap = requestMap.minus(entry.getKey());
            } else {
                entry.setValue(set);
            }
        }
    }

}
