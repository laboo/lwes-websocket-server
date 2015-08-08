package com.github.laboo.lwes.websocketserver;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.lwes.emitter.MulticastEventEmitter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by mlibucha on 5/9/15.
 */
public class Main extends WebSocketServer implements Runnable {
    public static int DEFAULT_PORT = 8887;
    private Map<WebSocket,Client> connToClientMap = new ConcurrentHashMap<>();
    private Map<String, Listener> channelToListenerMap = new ConcurrentHashMap<>();
    private Map<Client,String> clientToChannelMap = new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();
    private CommandLine cl;
    private Thread t;
    private static int port = DEFAULT_PORT;

    public Main(int port) {
        super(new InetSocketAddress(port));
    }

    public Main(int port, CommandLine cl) {
        this(port);
        this.port = port;
        this.cl = cl;
    }

    public static CommandLine parseArgs(String[] args) throws org.apache.commons.cli.ParseException {
        Options options = new Options();
        Option emit = Option.builder("e")
                .required(false)
                .desc("Emit events for testing purposes (false)")
                .longOpt("emit")
                .build();
        Option port = Option.builder("p")
                .required(false)
                .hasArg()
                .type(Integer.class)
                .desc("TCP port to listen on for WebSocket connections (8887)")
                .longOpt("port")
                .build();
        Option level = Option.builder("l")
                .required(false)
                .hasArg()
                .desc("Log level: trace, debug, info, warn, or error (warn)")
                .longOpt("log-level")
                .build();
        Option mbs = Option.builder("m")
                .required(false)
                .hasArg()
                .desc("Log rollover MB trigger (5)")
                .longOpt("log-mbs")
                .build();
        options.addOption(emit);
        options.addOption(port);
        options.addOption(level);
        options.addOption(mbs);
        // TODO log pattern
        DefaultParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    public static void main(String[] args) throws UnknownHostException, org.apache.commons.cli.ParseException {
        CommandLine cl = parseArgs(args);
        String port = cl.getOptionValue("port", String.valueOf(DEFAULT_PORT));
        new Main(Integer.parseInt(port),cl).go();
    }

    ExecutorService executor = Executors.newFixedThreadPool(5);

    public void go() throws org.apache.commons.cli.ParseException {
        String mbs = cl.getOptionValue('m', String.valueOf(Log.DEFAULT_ROLLOVER_MBS));
        Logger log = Log.getLogger(Integer.parseInt(mbs), Log.DEFAULT_PATTERN);
        String levelStr = this.cl.getOptionValue("l", Level.WARN.toString());
        Level level = determineLogLevel(levelStr);
        log.setLevel(Level.ALL);
        log.info("Log level from here on out is " + level);
        log.setLevel(level);

        this.start();

        long count = 0;
        MulticastEventEmitter emitter = null;
        try {
            if (cl != null && cl.hasOption("e")) {
                emitter = new MulticastEventEmitter();
                emitter.setESFFilePath("/path/to/esf/file");
                emitter.setMulticastAddress(InetAddress.getByName("224.0.0.69"));
                emitter.setMulticastPort(9191);
                emitter.initialize();
            }
            while (!Thread.currentThread().isInterrupted()) {
                if (cl != null && cl.hasOption("e")) {
                    org.lwes.Event e = null;
                    if (count % 2 == 0) {
                        e = emitter.createEvent("Click", false);
                        e.setString("url", "http://www.example.com");
                    } else if (count % 3 == 0) {
                        e = emitter.createEvent("Search", false);
                        e.setString("term", "the thing I'm searching for");
                        e.setDouble("lat", 51.5033630);
                        e.setDouble("lon", -0.1276250);
                    } else {
                        e = emitter.createEvent("Ad", false);
                        e.setString("text", "Buy my product");
                    }
                    e.setInt64("count", count++);
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
        Listener l = channelToListenerMap.get(channel);
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

    private Level determineLogLevel(String levelStr) throws ParseException {
        if (levelStr.equalsIgnoreCase("TRACE")) {
            return Level.TRACE;
        } else if (levelStr.equalsIgnoreCase("DEBUG")) {
            return Level.DEBUG;
        } else if (levelStr.equalsIgnoreCase("INFO")) {
            return Level.INFO;
        } else if (levelStr.equalsIgnoreCase("WARN")) {
            return Level.WARN;
        } else if (levelStr.equalsIgnoreCase("ERROR")) {
            return Level.ERROR;
        } else {
            throw new ParseException("Level [" + levelStr + "] not valid."
                    + " Choices are trace, debug, info, warn and error");
        }
    }

    synchronized private void assignClient(Client client) throws UnknownHostException {
        ClientConfig config = client.getConfig();
        String channel = config.getChannel();
        Listener l = channelToListenerMap.get(channel);
        if (l == null) {
            l = new Listener(config.getIp(), config.getPort());
            channelToListenerMap.put(channel, l);

            l.start();
        }
    }
}
