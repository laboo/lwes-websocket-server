package com.github.laboo.lwes.websocketserver;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;



/**
 * Created by mlibucha on 5/10/15.
 */
public class TestClientConfig {

    // Test equals(), hashcode(). These tests depend on ClientConfig.build(), thus @Test dependency

    @Test(dependsOnGroups = { "build" })
    public void testEquals() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        assertEquals(cc1, cc2);
        assertEquals(cc1.hashCode(), cc2.hashCode());
    }

    @Test(dependsOnGroups = { "build" })
    public void testEqualsWithFilters() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,"
                + "'filters':[{'name':'a','attribute':'b','value':'c'}],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,"
                + "'filters':[{'name':'a','attribute':'b','value':'c'}],'requests':{}}");
        assertEquals(cc1, cc2);
        assertEquals(cc1.hashCode(), cc2.hashCode());
    }

    @Test(dependsOnGroups = { "build" })
    public void testNotEqualsWithFilters() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,"
                + "'filters':[{'name':'a','attribute':'b','value':'c'}],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,"
                + "'filters':[{'name':'a','attribute':'b','value':'d'}],'requests':{}}");
        assertNotEquals(cc1, cc2);
    }

    @Test(dependsOnGroups = { "build" })
    public void testEqualsIrrelevantFieldsDiffer() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','maxSecs':99,'port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','batchSize':3,'port':1234,'filters':[],'requests':{}}");
        assertEquals(cc1, cc2);
        assertEquals(cc1.hashCode(), cc2.hashCode());
    }

    @Test(dependsOnGroups = { "build" })
    public void testEqualsDifferentIps() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'4.5.6.7','port':1234,'filters':[],'requests':{}}");
        assertNotEquals(cc1, cc2);
    }

    @Test(dependsOnGroups = { "build" })
    public void testEqualsDifferentPorts() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':4567,'filters':[],'requests':{}}");
        assertNotEquals(cc1, cc2);
    }

    @Test(dependsOnGroups = { "build" })
    public void testEqualsDifferentFilters() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,"
                + "'filters':[{'name':'x','attribute':'y','value':'z'}],'requests':{}}");
        assertNotEquals(cc1, cc2);
    }

    @Test(dependsOnGroups = { "build" })
    public void testEqualsDifferentRequests() throws Exception {
        ClientConfig cc1 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],'requests':{}}");
        ClientConfig cc2 = ClientConfig.build("{'ip':'1.2.3.4','port':1234,'filters':[],"
                + "'requests':{'x':['a','b','c']}}");
        assertNotEquals(cc1, cc2);
    }

    // Test the config syntax
    @Test(groups = {"build"})
    public void testSimplest() throws Exception {
        ClientConfig.build(readJson("simplest.json"));
    }

    @Test(groups = {"build"})
    public void testSimpler() throws Exception {
        ClientConfig config = ClientConfig.build(readJson("simpler.json"));
        assertEquals(config.getBatchSize(), 10);
        assertNotEquals(config.getRequests(),null);
    }

    @Test(groups = {"build"})
    public void testSimple() throws Exception {
        ClientConfig config = ClientConfig.build(readJson("simple.json"));
    }

    @Test(groups = {"build"},
            expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "port must be specified")
    public void testNoPort() throws Exception {
        ClientConfig.build(readJson("noPort.json"));
    }

    @Test(groups = {"build"},
            expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "port must be of type integer")
    public void testBadTypePort() throws Exception {
        ClientConfig.build(readJson("badTypePort.json"));
    }

    @Test(groups = {"build"},
            expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "ip must be of type string")
    public void testBadTypeIp() throws Exception {
        ClientConfig.build(readJson("badTypeIp.json"));
    }

    @Test(groups = {"build"},
            expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "ip must be specified")
    public void testNoIp() throws Exception {
        ClientConfig.build(readJson("noIp.json"));
    }

    @Test(groups = {"build"},
            expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = ".* is not a valid IP address")
    public void testBadFormatIp() throws Exception {
        ClientConfig.build(readJson("badFormatIp.json"));
    }

    @Test(groups = {"build"})
    public void testDefaults() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("simplest.json"));
        assertEquals(cc.getBatchSize(), ClientConfig.DEFAULT_BATCH_SIZE);
        assertEquals(cc.getMaxSecs(), ClientConfig.DEFAULT_MAX_SECS);
    }

    @Test(groups = {"build"},
            expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "requests must be specified")
    public void testNoRequests() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("noRequests.json"));
    }

    @Test(groups = {"build"},
            expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "requests must be of type \\(JSON\\) object")
    public void testBadRequestsArray() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("badRequestsArray.json"));
    }

    @Test(groups = {"build"},
            expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "requests must be of type \\(JSON\\) object, string to list of string")
    public void testBadRequestsStringToString() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("badRequestsStringToString.json"));
    }

    @Test(groups = {"build"},
            expectedExceptions = { IllegalArgumentException.class },
            expectedExceptionsMessageRegExp = "requests must be of type \\(JSON\\) object, string to list of string")
    public void testBadRequestsStringToIntArray() throws Exception {
        ClientConfig cc = ClientConfig.build(readJson("badRequestsStringToIntArray.json"));
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
