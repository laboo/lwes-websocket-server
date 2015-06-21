package com.github.laboo.lwes.websocketserver;

import ch.qos.logback.classic.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mlibucha on 5/10/15.
 */
public enum ConfigMap {
    INSTANCE;

    private static Logger log = Log.getLogger();
    private static Map<String, Set<ClientConfig>> requestMap;

    static {
        requestMap = Collections.unmodifiableMap(new HashMap<String,Set<ClientConfig>>());
    }

    synchronized private static void swapIn(Map<String,Set<ClientConfig>> newMap) {
        requestMap = Collections.unmodifiableMap(newMap);
    }

    public static Map<String, Set<ClientConfig>> getRequestMap() {
        return requestMap;
    }

    public synchronized static void print() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n---------------------\n");
        for (Map.Entry<String, Set<ClientConfig>> entry : requestMap.entrySet()) {
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
        log.info(sb.toString());
    }

    public synchronized static int sizeOfRequestMap() {
        return requestMap.size();
    }

    public synchronized static int numConfigs(String key) {
        Set<ClientConfig> set = requestMap.get(key);
        if (set == null) {
            return 0;
        }
        return set.size();
    }

    public synchronized static ClientConfig addClientConfig(ClientConfig clientConfig) {
        ClientConfig outConfig = clientConfig;
        Map<String,Set<ClientConfig>> newRequestMap = new HashMap<>();
        // Loop thru the existing map and make a copy of it.
        for (Map.Entry<String,Set<ClientConfig>> entry : requestMap.entrySet()) {
            String key = entry.getKey();
            Set<ClientConfig> oldSet = entry.getValue();
            Set<ClientConfig> newSet = new HashSet<ClientConfig>();
            for (ClientConfig cc : oldSet) {
                newSet.add(cc);
                // If we've already got an "equals" client config, we don't want to replace it
                // with the new one. We want to leave the old one in place, and later the connection
                // for the new client config will get added to the old client config.
                if (cc.equals(clientConfig)) {
                    outConfig = cc;
                    continue;
                }
                for (Map.Entry<String,List<String>> entry2 : clientConfig.getRequests().entrySet()) {
                    String requestKey = clientConfig.getChannel() + entry2.getKey();
                    if (key.equals(requestKey)) {
                        newSet.add(clientConfig);
                    }
                }
            }
            newRequestMap.put(key, Collections.unmodifiableSet(newSet));
        }

        // We also need to look through the entries in the ConfigClient because they might
        // contain requests for a new name we didn't pick up in the loop above for existing names.
        for (String name: clientConfig.getRequests().keySet()) {
            String requestKey = clientConfig.getChannel() + name;
            if (newRequestMap.get(requestKey) == null) {
                Set<ClientConfig> newSet = new HashSet<ClientConfig>();
                newSet.add(clientConfig);
                newRequestMap.put(requestKey, Collections.unmodifiableSet(newSet));
            }
        }

        swapIn(newRequestMap);
        return outConfig;
    }

    public synchronized static void removeClientConfig(ClientConfig clientConfig) {
        if (clientConfig.clients.size() != 0) {
            return;
        }
        Map<String,Set<ClientConfig>> newRequestMap = new HashMap<>();
        for (Map.Entry<String,Set<ClientConfig>> entry : requestMap.entrySet()) {
            String key = entry.getKey();
            Set<ClientConfig> oldSet = entry.getValue();
            Set<ClientConfig> newSet = new HashSet<ClientConfig>();
            for (ClientConfig cc : oldSet) {
                if (cc.equals(clientConfig)) {
                    continue; // this removes it by not copying it into our new copy of the set
                }
                newSet.add(cc);
            }
            if (newSet.isEmpty()) {
                continue; // this removes the requestMap(name) entry because it only had the one cc
            }
            newRequestMap.put(key, newSet);
        }
        swapIn(newRequestMap);
    }

}
