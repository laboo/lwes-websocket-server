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
        ClientConfig cc0 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[{'name':'a','attribute':'b','value':'c'}],'requests':{'x':['y']}}");
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{'x':['y']}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{'x':['y']}}");
        assertNotSame(cc1, cc2);
        ConfigMap.addClientConfig(cc0);
        ConfigMap.addClientConfig(cc1);
        ClientConfig back = ConfigMap.addClientConfig(cc2);
        assertSame(cc1, back);
    }
}
