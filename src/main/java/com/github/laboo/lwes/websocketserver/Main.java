package com.github.laboo.lwes.websocketserver;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.lwes.emitter.MulticastEventEmitter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by mlibucha on 5/9/15.
 */
public class Main extends WebSocketServer implements Runnable {
    public static int port = 8887;
    private Map<WebSocket,Client> connToClientMap = new ConcurrentHashMap<>();
    private Map<String, FilterListener> channelToListenerMap = new ConcurrentHashMap<>();
    private Map<Client,String> clientToChannelMap = new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();
    private String[] args;
    private Thread t;

    public Main() {super(new InetSocketAddress(port));}

    public Main(String[] args) {
        this();
        this.args = args;
    }

    public static CommandLine parseArgs(String[] args) throws org.apache.commons.cli.ParseException {
        Options options = new Options();
        options.addOption("e", "--emit", false, "Emit events for testing purposes");
        BasicParser parser = new BasicParser();
        return parser.parse(options, args);
    }

    public void start() {
        t = new Thread(new Main(this.args));
        t.start();
    }

    public void stop() {
        t.interrupt();
    }

    public static void main(String[] args) throws UnknownHostException, org.apache.commons.cli.ParseException {
        new Main().run(args);
    }

    ExecutorService executor = Executors.newFixedThreadPool(5);

    public void run(String args[]) throws org.apache.commons.cli.ParseException {
        Logger log = Log.getLogger();
        log.info("info help");
        CommandLine cl = parseArgs(args);
        Main server = new Main();
        server.start();

        int count = 0;
        MulticastEventEmitter emitter = null;
        try {
            if (cl.hasOption("e")) {
                emitter = new MulticastEventEmitter();
                emitter.setESFFilePath("/path/to/esf/file");
                emitter.setMulticastAddress(InetAddress.getByName("224.0.0.69"));
                emitter.setMulticastPort(9191);
                emitter.initialize();
            }

            while (!Thread.currentThread().isInterrupted()) {
                if (cl.hasOption("e")) {
                    org.lwes.Event e = emitter.createEvent("MyEvent", false);
                    e.setString("key", String.valueOf(count++));
                    emitter.emit(e);
                }
                Thread.sleep(1000);
            }
        } catch (IOException ioe) {
            log.error(ioe.toString());
        } catch (InterruptedException ie) {
            log.info("Interrupted. Quitting.");
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Client client = new Client(conn);
        connToClientMap.put(conn, client);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ClientConfig config = null;
        try {
            config = ClientConfig.build(message);
        } catch (Exception e) {
            Response response =
                    new Response(Response.ERROR_TYPE, e.getMessage() + " in " + message, new ArrayList<Event>());
            try {
                String data = mapper.writeValueAsString(response);
                conn.send(data);
            } catch (Exception e2) {
                conn.send("Invalid JSON: \n"
                        + e.getMessage() + "\n"
                        + message);
            }
            conn.close(1011, "Invalid client request");
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
        ConfigMap.print();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
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
