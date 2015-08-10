package com.github.laboo.lwes.websocketserver;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.server.Server;
import org.lwes.emitter.MulticastEventEmitter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by mlibucha on 5/9/15.
 */
public class Main {
    public static int DEFAULT_PORT = 8887;
    private Server server;
    private CommandLine cl;
    private Thread t;
    private static int port = DEFAULT_PORT;
    private static Options options;

    public Main(int port) {
        System.out.println("help");
        this.port = port;
        // TODO wtf is this map???
        this.server = new Server("localhost", port, "/", new HashMap<String, Object>(),
                LwesWebSocketEndpoint.class);
        System.out.println("created");
    }

    public Main(int port, CommandLine cl) {
        this(port);
        this.cl = cl;
    }

    public void start() throws DeploymentException {
        System.out.println("starting");
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }

    public static CommandLine parseArgs(String[] args) throws org.apache.commons.cli.ParseException {
        options = new Options();

        Option help = Option.builder("h")
                .required(false)
                .desc("Show help.")
                .longOpt("help")
                .build();
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
        options.addOption(help);
        options.addOption(emit);
        options.addOption(port);
        options.addOption(level);
        options.addOption(mbs);
        // TODO log pattern
        DefaultParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    public static void main(String[] args) throws
            UnknownHostException, org.apache.commons.cli.ParseException, DeploymentException {
        CommandLine cl = parseArgs(args);
        if (cl.hasOption("h"))  {
            HelpFormatter formater = new HelpFormatter();
            formater.printHelp("Main", options);
            return;
        }
        String port = cl.getOptionValue("port", String.valueOf(DEFAULT_PORT));
        new Main(Integer.parseInt(port),cl).go();
    }

    ExecutorService executor = Executors.newFixedThreadPool(5);

    public void go() throws org.apache.commons.cli.ParseException, DeploymentException {
        String mbs = cl.getOptionValue('m', String.valueOf(Log.DEFAULT_ROLLOVER_MBS));
        Logger log = Log.getLogger(Integer.parseInt(mbs), Log.DEFAULT_PATTERN);
        String levelStr = this.cl.getOptionValue("l", Level.WARN.toString());
        Level level = determineLogLevel(levelStr);
        log.setLevel(Level.ALL);
        log.info("Log level from here on out is " + level);
        log.setLevel(level);

        //Server server = new Server("localhost", 8025, "/websocket", LwesWebSocketEndpoint.class);

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
        this.stop();
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

}
