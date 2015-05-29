package com.github.laboo.lwes.websocketserver;

import static org.testng.Assert.*;

import com.fasterxml.jackson.core.json.WriterBasedJsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

/**
 * Created by mlibucha on 5/10/15.
 */
public class TestClientConfig {
    private static ObjectMapper mapper = new ObjectMapper();

    String simplest = "{"
            +  "\"batchSize\": 1,"
            +   "\"ip\": \"1.2.3.4\","
            +   "\"port\": 1111,"
            +   "\"requests\": {"
            +   "},"
            +   "\"filters\": ["
            +   "]"
            +"}";

    String simpler = "{"
            +  "\"batchSize\":10,"
            +   "\"ip\":\"1.2.3.4\","
            +   "\"port\":1111,"
            +   "\"requests\": {"
            +     "\"food\":["
            +     "],"
            +     "\"drink\":["
            +     "]"
            +   "},"
            +   "\"filters\":["
            +     "{"
            +       "\"eventName\":\"food\","
            +       "\"attribute\":\"taste\","
            +       "\"regex\":\"hot\""
            +     "}"
            +   "]"
            +"}";

    String simple = "{"
            +  "\"batchSize\":10,"
            +   "\"ip\":\"1.2.3.4\","
            +   "\"port\":1111,"
            +   "\"requests\": {"
            +     "\"food\":["
            +       "\"burger\",\"fries\""
            +     "],"
            +     "\"drink\":["
            +       "\"Coke\""
            +     "]"
            +   "},"
            +   "\"filters\":["
            +     "{"
            +       "\"eventName\":\"food\","
            +       "\"attribute\":\"taste\","
            +       "\"regex\":\"hot\""
            +     "},"
            +     "{"
            +       "\"eventName\":\"drink\","
            +       "\"attribute\":\"taste\","
            +       "\"regex\":\"dry\""
            +     "}"
            +   "]"
            +"}";

    @Test
    public void show() throws Exception {
        ClientConfig cc = new ClientConfig();
        Filter f = new Filter("food", "taste", "^hot$");
        Filter[] filters = new Filter[1];
        filters[0] = f;
        cc.setFilters(filters);
        System.out.println(mapper.writeValueAsString(cc));
    }

    @Test
    public void testSimplest() throws Exception {
        ClientConfig config = mapper.readValue(simplest, ClientConfig.class);
    }

    @Test
    public void testSimpler() throws Exception {
        ClientConfig config = mapper.readValue(simpler, ClientConfig.class);
        assertEquals(config.getBatchSize(), 10);
        assertNotEquals(config.getRequests(),null);
    }

    @Test
    public void testSimple() throws Exception {
        ClientConfig config = mapper.readValue(simple, ClientConfig.class);
    }
}
