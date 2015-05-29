package com.github.laboo.lwes.websocketserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.util.regex.PatternSyntaxException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * Created by mlibucha on 5/17/15.
 */
public class TestFilter {

    private static ObjectMapper mapper = new ObjectMapper();
    static Filter f1 = new Filter("a", "b", "c");
    static Filter f2 = new Filter("a", "b", "c");
    static Filter f3 = new Filter("x", "y", "z");
    static String filtersStr = "["
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
            +   "]";
    static String badfiltersStr1 = "["
            +     "{"
            +       "\"attribute\":\"taste\","
            +       "\"regex\":\"hot\""
            +     "},"
            +     "{"
            +       "\"eventName\":\"drink\","
            +       "\"attribute\":\"taste\","
            +       "\"regex\":\"dry\""
            +     "}"
            +   "]";

    @Test
    public void testEquals() throws Exception {
        assertEquals(f1, f2);
        assertNotEquals(f1, f3);
    }

    @Test
    public void testHashCode() {
        assertEquals(f1.hashCode(), f2.hashCode());
        assertNotEquals(f1.hashCode(), f3.hashCode());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNull() {
        Filter fnull = new Filter(null, "b", "c");
        fnull.wellFormedCheck();
    }

    @Test
    public void testGoodMapping() throws Exception {
        Filter[] filters;
        mapper.readValue(filtersStr, Filter[].class);
    }

    @Test(expectedExceptions = {PatternSyntaxException.class})
    public void testBadRegex() throws Exception {
        Filter bad = new Filter("a", "b", "*\\.*");
    }

    // Can't do this until Jackson 2.6
    // @Test(expectedExceptions = SomeJsonExceptionForRequiredClass.class)
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadMapping1() throws Exception {
        Filter[] filters = mapper.readValue(badfiltersStr1, Filter[].class);
        for (Filter filter : filters) {
            filter.wellFormedCheck();
        }
    }

    @Test(enabled = false)
    public void test() throws Exception {
        Filter bad = new Filter("a", "b", "*\\.*");
    }
}
