package com.github.laboo.lwes.websocketserver;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by mlibucha on 5/16/15.
 */

@JsonIgnoreProperties({"pattern"})
public class Filter {
    private static Logger log = Log.getLogger();
    private String name;
    private String attribute;
    private String value;
    private Pattern namePattern;
    private Pattern valuePattern;

    public Filter() {}

    public Filter(String name, String attribute, String value) {
        this.name = name;
        this.attribute = attribute;
        this.value = value;
        wellFormedCheck();
        this.setName(name);
        this.setAttribute(attribute);
        this.setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.namePattern = Pattern.compile(name);
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;

    }

    public void setValue(String value) {
        this.value = value;
        this.valuePattern = Pattern.compile(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Filter filter = (Filter) o;

        if (!name.equals(filter.name)) return false;
        if (!attribute.equals(filter.attribute)) return false;
        return value.equals(filter.value);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + attribute.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    public boolean matches(org.lwes.Event event) {
        if (!namePattern.matcher(event.getEventName()).matches()) {
            log.trace("name doesn't match");
            return false;
        }

        Object value = event.get(attribute);
        if (value == null) {
            log.trace("value is null for " + attribute);
            return false;
        }

        return valuePattern.matcher(String.valueOf(value)).matches();
    }

    @Override
    public String toString() {
        return "{name:" + name + ",attribute:" + attribute + ",value:" + value + "}";
    }

    public void wellFormedCheck() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is required in filter: " + this);
        }
        if (attribute == null || attribute.isEmpty()) {
            throw new IllegalArgumentException("attribute is required in filter: " + this);
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("value is required in filter: " + this);
        }
    }
}
