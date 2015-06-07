package com.github.laboo.lwes.websocketserver;

import java.util.List;
import java.util.Map;

/**
 * Created by mlibucha on 6/6/15.
 */
public class Response {
    public static String DATA_TYPE = "data";
    public static String ERROR_TYPE = "error";
    String type;
    String msg;
    List<Event> data;

    public Response(List<Event> data) {
        this(DATA_TYPE, "", data);
    }

    public Response(String type, String msg, List<Event> data) {
        this.type = type;
        this.msg = msg;
        this.data = data;
    }

    public String getType() {
        return this.type;
    }

    public String getMsg() {
        return this.msg;
    }

    public List<Event> getData() {
        return this.data;
    }
}
