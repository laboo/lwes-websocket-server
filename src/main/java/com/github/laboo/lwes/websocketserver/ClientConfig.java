package com.github.laboo.lwes.websocketserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by mlibucha on 5/9/15.
 */

@JsonIgnoreProperties({"events", "clients"})
public class ClientConfig {
    int batchSize;
    int maxSecs;
    String ip;
    int port;
    Filter[] filters = new Filter[0];
    Map<String, String[]> requests;
    Set<Client> clients = new HashSet<Client>();

    public ClientConfig() {}

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

    public Filter[] getFilters() {
        return filters;
    }

    public void setFilters(Filter[] filters) {
        this.filters = filters;
    }

    public void setRequests(Map<String, String[]> requests) {
        this.requests = requests;
    }

    public Map<String,String[]> getRequests() {
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
        if (!Arrays.deepEquals(filters, that.filters)) return false;
        if (requests.size() != that.requests.size()) return false;
        for (Map.Entry<String, String[]> entry : that.requests.entrySet()) {
            if (requests.get(entry.getKey()) == null ) return false;
            if (!Arrays.deepEquals(requests.get(entry.getKey()), entry.getValue())) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + port;
        result = 31 * result + Arrays.deepHashCode(filters);
        for (Map.Entry<String,String[]> entry : requests.entrySet()) {
            result = 31 * result + entry.getKey().hashCode();
            result = 31 * result + Arrays.deepHashCode(entry.getValue());
        }
        return result;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{channel: ");
        sb.append(this.getChannel());
        sb.append(" filters: [");
        for (Filter f : filters) {
            sb.append("  ");
            sb.append(f);
        }
        sb.append("],");
        sb.append(" requests:");
        for (Map.Entry<String,String[]> entry : requests.entrySet()) {
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
        sb.append("}");
        return sb.toString();
    }

}
