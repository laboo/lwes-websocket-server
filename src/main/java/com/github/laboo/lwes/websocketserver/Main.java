package com.github.laboo.lwes.websocketserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.lwes.emitter.MulticastEventEmitter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mlibucha on 5/9/15.
 */
public class Main extends WebSocketServer {
    public static int port = 8887;
    private Map<WebSocket,Client> connToClientMap = new ConcurrentHashMap<>();
    private Map<String, FilterListener> channelToListenerMap = new ConcurrentHashMap<>();
    private Map<Client,String> clientToChannelMap = new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();
    public Main() {super(new InetSocketAddress(port));}

    public static void main(String[] args) throws UnknownHostException {
        int count = 0;
        Main server = new Main();
        server.start();
        try {
            //server.wait();
            MulticastEventEmitter emitter = new MulticastEventEmitter();
            emitter.setESFFilePath("/path/to/esf/file");
            emitter.setMulticastAddress(InetAddress.getByName("224.0.0.69"));
            emitter.setMulticastPort(9191);
            emitter.initialize();

            while (true) {
                org.lwes.Event e = emitter.createEvent("MyEvent", false);
                e.setString("key", String.valueOf(count++));
                emitter.emit(e);
                Thread.sleep(1000);
            }

        } catch (IOException ioe) {
            System.out.println(ioe);
        } catch (InterruptedException ie) {
            System.out.println("Interrupted. Quitting.");
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("got connection");
        Client client = new Client(conn);
        connToClientMap.put(conn, client);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ClientConfig config = null;
        System.out.println("got message");
        try {
            config = mapper.readValue(message, ClientConfig.class);
        } catch (Exception e) {
            // TODO can we do better here?
            conn.send("bad config: " + e);
            return;
        }

        Client client = connToClientMap.get(conn);
        client.setBatchSize(config.getBatchSize());
        client.setMaxSecs(config.getMaxSecs());
        config = ConfigMap.addClientConfig(config);
        client.setConfig(config);
        config.addClient(client);
        clientToChannelMap.put(client, config.getChannel());

        try {
            assignClient(client);
        } catch (UnknownHostException uhe) {
            conn.send("Unknown host: " + config.getIp());
        } catch (Exception e) {
            conn.send("Exception parsing config: " + config);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closing: code=" + code + " reason=" + reason + " remote=" + remote);
        close(conn);
        ConfigMap.print();
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        e.printStackTrace();
        close(conn); // TODO Research if this is necessary.
    }

    synchronized private void close(WebSocket conn) {
        Client client = connToClientMap.remove(conn);
        if (client == null) {
            return;
        }
        ClientConfig config = client.getConfig();
        if (config == null) {
            return;
        }
        config.removeClient(client);
        ConfigMap.removeClientConfig(config);
        String channel = client.getConfig().getChannel();

        clientToChannelMap.remove(client);
        FilterListener l = channelToListenerMap.get(channel);
        if (l != null) {
            boolean destroy = true;
            for (String ch : clientToChannelMap.values()) {
                if (ch.equals(channel)) {
                    destroy = false;
                    break;
                }
            }
            if (destroy) {
                l = channelToListenerMap.remove(channel);
                l.destroy();;
            }
        }
        conn.close();
    }

    synchronized private void assignClient(Client client) throws UnknownHostException {
        ClientConfig config = client.getConfig();
        String channel = config.getChannel();
        FilterListener l = channelToListenerMap.get(channel);
        if (l == null) {
            l = new FilterListener(config.getIp(), config.getPort());
            channelToListenerMap.put(channel, l);
            l.start();
        }
    }
}
