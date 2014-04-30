package org.jboss.as.test.manualmode.web.websocket;

import org.jboss.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author rhatlapa (rhatlapa@redhat.com)
 */
@ClientEndpoint
public class WebSocketClient {
    private static Logger log = Logger.getLogger(WebSocketClient.class);
    public static CountDownLatch latch;
    public static List<String> history = new ArrayList<String>();

    @OnMessage
    public void onMessage(String message) {
        log.info("Received message: " + message);
        history.add(message);
        latch.countDown();
    }

    @OnOpen
    public void onOpen(Session session) {
        log.info("Client joined websocket session");
    }

    @OnClose
    public void onClose(Session session) {
        log.info("Client left websocket session");
    }
}
