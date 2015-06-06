package com.github.laboo.lwes.websocketserver;

import static org.testng.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by mlibucha on 5/10/15.
 */
public class TestClientConfig {
    private static ObjectMapper mapper = new ObjectMapper();

    // Test equals(), hashcode

    @Test
    public void testEquals() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        assertEquals(cc1, cc2);
    }

    @Test
    public void testEqualsIrrelevantFieldsDiffer() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','maxSecs':99,'port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','batchSize':3,'port':1234,'filters':[],'requests':{}}");
        assertEquals(cc1, cc2);
    }

    @Test
    public void testEqualsDifferentIps() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'4.5.6.7','port':1234,'filters':[],'requests':{}}");
        assertNotEquals(cc1, cc2);
    }

    @Test
    public void testEqualsDifferentPorts() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':4567,'filters':[],'requests':{}}");
        assertNotEquals(cc1, cc2);
    }

    @Test
    public void testEqualsDifferentFilters() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,"
                + "'filters':[{'name':'x','attribute':'y','value':'z'}],'requests':{}}");
        assertNotEquals(cc1, cc2);
    }

    @Test
    public void testEqualsDifferentRequests() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],"
                + "'requests':{'x':['a','b','c']}}");
        assertNotEquals(cc1, cc2);
    }

    // Test the config syntax
    @Test
    public void testSimplest() throws Exception {
        ClientConfig.build(readJson("simplest.json"));
    }

    @Test
    public void testSimpler() throws Exception {
        ClientConfig config = ClientConfig.build(readJson("simpler.json"));
        assertEquals(config.getBatchSize(), 10);
        assertNotEquals(config.getRequests(),null);
    }

    @Test
    public void testSimple() throws Exception {
        ClientConfig config = ClientConfig.build(readJson("simple.json"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "port must be specified")
    public void testNoPort() throws Exception {
        ClientConfig.build(readJson("noPort.json"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "port must be of type integer")
    public void testBadTypePort() throws Exception {
        ClientConfig.build(readJson("badTypePort.json"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "ip must be of type string")
    public void testBadTypeIp() throws Exception {
        ClientConfig.build(readJson("badTypeIp.json"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "ip must be specified")
    public void testNoIp() throws Exception {
        ClientConfig.build(readJson("noIp.json"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = ".* is not a valid IP address")
    public void testBadFormatIp() throws Exception {
        ClientConfig.build(readJson("badFormatIp.json"));
    }

    @Test
    public void testDefaults() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("simplest.json"));
        assertEquals(cc.getBatchSize(), ClientConfig.DEFAULT_BATCH_SIZE);
        assertEquals(cc.getMaxSecs(), ClientConfig.DEFAULT_MAX_SECS);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "requests must be specified")
    public void testNoRequests() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("noRequests.json"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "requests must be of type \\(JSON\\) object")
    public void testBadRequestsArray() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("badRequestsArray.json"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "requests must be of type \\(JSON\\) object, string to list of string")
    public void testBadRequestsStringToString() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("badRequestsStringToString.json"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "requests must be of type \\(JSON\\) object, string to list of string")
    public void testBadRequestsStringToIntArray() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("badRequestsStringToIntArray.json"));
    }

    @Test
    public void test() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("simple.json"));
    }

    public String readJson(String filename) throws Exception {
        File file = new File("src/main/resources/" + filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return new String(data, "UTF-8");
    }
}
