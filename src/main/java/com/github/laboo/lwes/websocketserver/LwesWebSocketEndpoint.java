package com.github.laboo.lwes.websocketserver;

/**
 * Created by mlibucha on 8/8/15.
 */
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@ServerEndpoint(value = "/lwes") // TODO should really be something other than just /
public class LwesWebSocketEndpoint {
    private static Logger log = Log.getLogger();
    private static Map<Session,Client> sessionToClientMap = new ConcurrentHashMap<>();
    private static Map<String, Listener> channelToListenerMap = new ConcurrentHashMap<>();
    private static Map<Client,String> clientToChannelMap = new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();

    @OnOpen
    public void onOpen(final Session session) {
        Client client = new Client(session);
        sessionToClientMap.put(session, client);
    }

    @OnMessage
    public void getMessage(final String message, final Session session) {
        ClientConfig config = null;
        try {
            config = ClientConfig.build(message);
        } catch (Exception e) {
            Response response =
                    new Response(Response.ERROR_TYPE, e.getMessage() + " in " + message, new ArrayList<Event>());
            try {
                String data = mapper.writeValueAsString(response);
                session.getBasicRemote().sendText(data);
            } catch (Exception e2) {
                try {
                    session.getBasicRemote().sendText("Invalid JSON: \n"
                            + e.getMessage() + "\n"
                            + message);
                    session.close(new CloseReason(CloseReason.CloseCodes.NOT_CONSISTENT,
                            "Invalid JSON."));
                } catch (IOException ioe) {
                    log.warn("Error closing websocket session after bad JSON request");
                }
            }
            return;
        }
        Client client = sessionToClientMap.get(session);
        client.setBatchSize(config.getBatchSize());
        client.setMaxSecs(config.getMaxSecs());
        config = ConfigMap.addClientConfig(config);
        client.setConfig(config);
        config.addClient(client);
        clientToChannelMap.put(client, config.getChannel());
        String error = null;
        try {
            assignClient(client);
        } catch (UnknownHostException uhe) {
            error = "Unknown host: " + config.getIp();
        } catch (Exception e) {
            error = "Exception parsing config: " + config;
        }
        if (error != null) {
            try {
                session.getBasicRemote().sendText(error);
            } catch (IOException e) {
                log.warn("Error sending on websocket session at session start", e);
            }
        }
        ConfigMap.print();
    }

    @OnError
    public void onError(Session session, Throwable t) {
        log.warn("Got error.", t);
        close(session); // TODO research
    }

    @OnClose
    public void closeConnectionHandler(Session session, CloseReason closeReason) {
        close(session);
        ConfigMap.print();
    }

    synchronized private void close(Session session) {
        Client client = sessionToClientMap.remove(session);
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
                l.destroy();
            }
        }
        try {
            session.close();
        } catch (IOException ioe) {
            log.warn("Error closing websocket session at session end", ioe);
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
