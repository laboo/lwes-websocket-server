package com.github.laboo.lwes.websocketserver;

import static org.testng.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.krukow.clj_lang.PersistentHashSet;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by mlibucha on 5/17/15.
 */
public class TestConfigMap {

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    public static void testSimple() throws Exception {
        ClientConfig cc = new ClientConfig();
        Filter[] filters = new Filter[1];
        filters[0] = new Filter("a", "b", "c");
        cc.setFilters(filters);
        Map<String,String[]> requests = new HashMap<>();
        String[] array = {"y", "z"};
        requests.put("x", array);
        cc.setRequests(requests);
        cc.setIp("1.1.1.1");
        cc.setPort(1111);
        cc.setBatchSize(20);
        assertTrue(ConfigMap.sizeOfRequestMap() == 0);
        ConfigMap.addClientConfig(cc);
        assertTrue(ConfigMap.sizeOfRequestMap() == 1);
        ConfigMap.print();
        org.lwes.Event event = new org.lwes.ArrayEvent();
        event.setEventName("x");
        event.setString("y", "a");
        event.setString("z", "b");
        int matches = ConfigMap.handleEvent(event, cc.getChannel());
        assertTrue(matches == 1, "matches mismatch");
        System.out.println("matches=" + matches);
        ConfigMap.removeClientConfig(cc);
        assertTrue(ConfigMap.sizeOfRequestMap() == 0);
        assertEquals(cc.events.size(), 1);
        Event pulledEvent = cc.events.take();
        System.out.println("Event:" + pulledEvent);
    }
}
