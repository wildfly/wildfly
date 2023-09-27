/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.websocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

import io.undertow.util.FlexBase64;

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
                    .getBytes(StandardCharsets.UTF_8), false)));
        }
    }
}
