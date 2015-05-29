package com.github.laboo.lwes.websocketserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mlibucha on 5/10/15.
 */
public enum ConfigMap {
    INSTANCE;

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
        System.out.println("---------------------");
        for (Map.Entry<String, Set<ClientConfig>> entry : requestMap.entrySet()) {
            System.out.print(entry.getKey() + " => [");
            for (ClientConfig cc : entry.getValue()) {
                System.out.print(cc);
            }
            System.out.println("]");
        }
        System.out.println("---------------------");
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
        System.out.println("adding cc " + clientConfig.getChannel());
        Map<String,Set<ClientConfig>> newRequestMap = new HashMap<>();
        // Loop thru the existing map and make a copy of it.
        for (Map.Entry<String,Set<ClientConfig>> entry : requestMap.entrySet()) {
            String key = entry.getKey();
            Set<ClientConfig> oldSet = entry.getValue();
            Set<ClientConfig> newSet = new HashSet<ClientConfig>();
            for (ClientConfig cc : oldSet) {
                newSet.add(cc);
                // XXX document
                if (cc.equals(clientConfig)) {
                    outConfig = cc;
                    continue;
                }
                for (Map.Entry<String,String[]> entry2 : clientConfig.getRequests().entrySet()) {
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