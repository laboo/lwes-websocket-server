package com.github.laboo.lwes.websocketserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.net.InetAddresses;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by mlibucha on 5/9/15.
 */

@JsonIgnoreProperties({"events", "clients"})
public class ClientConfig {
    public static String BATCH_SIZE_KEY = "batchSize";
    public static String MAX_SECS_KEY = "maxSecs";
    public static String IP_KEY = "ip";
    public static String PORT_KEY = "port";
    public static String REQUESTS_KEY = "requests";
    public static String FILTERS_KEY = "filters";
    public static int DEFAULT_BATCH_SIZE = 100;
    public static int DEFAULT_MAX_SECS = 60;

    int batchSize;
    int maxSecs;
    String ip;
    int port;
    List<Filter> filters = new ArrayList<>();
    Map<String, List<String>> requests;
    Set<Client> clients = new HashSet<Client>();

    private ClientConfig(String ip,
                         int port,
                         int batchSize,
                         int maxSecs,
                         List<Filter> filters,
                         Map<String, List<String>> requests) {
        this.ip = ip;
        this.port = port;
        this.batchSize = batchSize;
        this.maxSecs = maxSecs;
        this.filters = filters;
        this.requests = requests;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getMaxSecs() {
        return maxSecs;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setMaxSecs(int maxSecs) {
        this.maxSecs = maxSecs;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public void setRequests(Map<String, List<String>> requests) {
        this.requests = requests;
    }

    public Map<String,List<String>> getRequests() {
        return this.requests;
    }

    public String getChannel() {
        return this.ip + ":" + this.port;
    }

    public void addClient(Client client) {
        this.clients.add(client);
        System.out.println("Clients: " + clients.size());
    }

    public boolean removeClient(Client client) {
        return this.clients.remove(client);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientConfig that = (ClientConfig) o;
        if (port != that.port) return false;
        if (!ip.equals(that.ip)) return false;
        if (filters.hashCode() != that.filters.hashCode()) return false; // XXX
        if (requests.size() != that.requests.size()) return false;
        for (Map.Entry<String, List<String>> entry : that.requests.entrySet()) {
            if (requests.get(entry.getKey()) == null ) return false;
            if (!(requests.get(entry.getKey()).hashCode() == entry.getValue().hashCode())) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + port;
        result = 31 * result + filters.hashCode(); // XXX
        for (Map.Entry<String,List<String>> entry : requests.entrySet()) {
            result = 31 * result + entry.getKey().hashCode();
            for (String str : entry.getValue()) {
                result = 31 * result + str.hashCode(); // XXX
            }
        }
        return result;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("channel: ");
        sb.append(this.getChannel());
        sb.append(" filters: [");
        if (filters != null) {
            for (Filter f : filters) {
                sb.append("  ");
                sb.append(f);
            }
            sb.append("],");
        }
        sb.append(" requests:");
        if (requests != null) {
            for (Map.Entry<String, List<String>> entry : requests.entrySet()) {
                sb.append("  ");
                sb.append(entry.getKey());
                sb.append("=>[");
                boolean first = true;
                for (String attr : entry.getValue()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append(attr);
                }
                sb.append("]");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static ClientConfig build(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        Map<String,Object> config = mapper.readValue(json, Map.class);


        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));


        int batchSize = DEFAULT_BATCH_SIZE;
        int maxSecs = DEFAULT_MAX_SECS;
        String ip = null;
        Map<String, List<String>> requests = new HashMap<>();
        List<Filter> filters = new ArrayList<>();

        Integer port = null;

        try {
            Object obj = config.get(BATCH_SIZE_KEY);
            if (obj != null) {
                batchSize = (Integer) obj;
            }
        } catch (ClassCastException cce) {
            throw new IllegalArgumentException(BATCH_SIZE_KEY + " must be of type integer");
        }

        try {
            Object obj = config.get(MAX_SECS_KEY);
            if (obj != null) {
                maxSecs = (Integer) obj;
            }
        } catch (ClassCastException cce) {
            throw new IllegalArgumentException(MAX_SECS_KEY + " must be of type integer");
        }

        try {
            port = (Integer) config.get(PORT_KEY);
        } catch (ClassCastException cce) {
            throw new IllegalArgumentException(PORT_KEY + " must be of type integer");
        }
        if (port == null) {
            throw new IllegalArgumentException(PORT_KEY + " must be specified");
        }

        try {
            ip = (String) config.get(IP_KEY);
        } catch (ClassCastException cce) {
            throw new IllegalArgumentException(IP_KEY + " must be of type string");
        }
        if (ip == null) {
            throw new IllegalArgumentException(IP_KEY + " must be specified");
        }
        try {
            InetAddresses.forString(ip);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(ip + " is not a valid IP address");
        }

        try {
            requests = (Map) config.get(REQUESTS_KEY);
        } catch (ClassCastException cce) {
            String msg = REQUESTS_KEY + " must be of type (JSON) object";
            System.out.println("*" + msg + "*");
            throw new IllegalArgumentException(REQUESTS_KEY + " must be of type (JSON) object");
        }

        if (requests == null) {
            throw new IllegalArgumentException(REQUESTS_KEY + " must be specified");
        }

        try {
            Map<String, List<String>> r = (Map<String, List<String>>) config.get(REQUESTS_KEY);
            for (Map.Entry<String, List<String>> entry : r.entrySet()) {
                for (String entry2 : entry.getValue()) {
                    // pass
                }
            }
        } catch (ClassCastException cce) {
            throw new IllegalArgumentException(REQUESTS_KEY + " must be of type (JSON) object, string to list of string");
        }

        try {

            Object obj = config.get(FILTERS_KEY);
            if (obj != null) {
                if (!(obj instanceof List)) {
                    throw new IllegalArgumentException(FILTERS_KEY + " must be a list of objects");
                }

                List<Object> olist = (List<Object>) obj;
                for (Object o: olist) {
                    System.out.println(o);
                    if (!(o instanceof Map)) {
                        throw new IllegalArgumentException(FILTERS_KEY + " list must contain object types");
                    }
                    Map map = (Map) o;
                    System.out.println(map.get("name"));
                    System.out.println(map.get("attribute"));
                    System.out.println(map.get("value"));

                    filters.add(new Filter((String) map.get("name"),
                            (String) map.get("attribute"),
                            (String) map.get("value")));
                    //filters.add(f);
                }
            }
        } catch (ClassCastException cce) {
            System.out.println(cce);
            throw new IllegalArgumentException(FILTERS_KEY + " must be of type object");
        }

        return new ClientConfig(
                ip,
                port,
                batchSize,
                maxSecs,
                filters,
                requests
        );
    }
}
