package org.jboss.as.test.integration.web.security.websocket;

import io.undertow.util.FlexBase64;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author Stuart Douglas
 */
@ClientEndpoint(configurator = AnnotatedClient.AuthConfigurator.class)
public class AnnotatedClient {
    private static String user;
    private static String password;

    private final BlockingDeque<String> queue = new LinkedBlockingDeque<>();

    @OnOpen
    public void open(final Session session) throws IOException {
        session.getBasicRemote().sendText("Hello");
    }

    @OnMessage
    public void message(final String message) {
        queue.add(message);
    }

    public String getMessage() throws InterruptedException {
        return queue.poll(5, TimeUnit.SECONDS);
    }

    public void setCredentials(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public static class AuthConfigurator extends ClientEndpointConfig.Configurator {
        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            String credentials = user + ":" + password;
            headers.put("AUTHORIZATION", Collections.singletonList("Basic " + FlexBase64.encodeString(credentials
                    .getBytes(), false)));
        }
    }
}
