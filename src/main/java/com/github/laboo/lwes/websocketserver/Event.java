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

    public boolean equals(org.lwes.Event e) {
        if (!this.name.equals(e.getEventName())) {
            return false;
        }
        int extra = 0;

        // These are "internal" attributes that may be set on an org.lwes.Event
        if (e.get("enc") != null) {
            extra++;
        }
        if (e.get("ReceiptTime") != null) {
            extra++;
        }
        if (e.get("SenderIP") != null) {
            extra++;
        }
        if (e.get("SenderPort") != null) {
            extra++;
        }

        if (attrs.size() + extra != e.getNumEventAttributes()) { // enc
            return false;
        }
        for (String attr : attrs.keySet()) {
            Object evalue = e.get(attr);
            if (evalue == null || !evalue.equals(attrs.get(attr))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException jpe) {
            return super.toString();
        }
    }

}
