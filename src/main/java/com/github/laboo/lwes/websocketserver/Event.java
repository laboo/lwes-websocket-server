package com.github.laboo.lwes.websocketserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mlibucha on 5/9/15.
 */
public class Event {
    private static ObjectMapper mapper = new ObjectMapper();

    String name;
    Map<String,Object> attrs;

    public Event() {}
    public Event(String name) {
        this(name, new HashMap<String,Object>());
    }

    public Event(String name, Map<String, Object> attrs) {
        this.name = name;
        this.attrs = attrs;
    }

    public String getName() {
        return this.name;
    }

    public Map<String,Object> getAttrs() {
        return attrs; // TODO read only version
    }

    public void setAttr(String attr, Object value) {
        attrs.put(attr, value);
    }

    @Override
    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException jpe) {
            return super.toString();
        }
        /*
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("\n");
        for (Map.Entry<String,Object> entry : attrs.entrySet()) {
            sb.append("  ");
            sb.append(entry.getKey());
            sb.append(" => ");
            sb.append(entry.getValue());
            sb.append("\n");
        }
        return sb.toString();
        */
    }

}
