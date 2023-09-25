/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.websocket;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.AroundConstructBinding;
import org.jboss.as.test.integration.ee.injection.support.Bravo;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptorBinding;
import org.jboss.as.test.integration.ee.injection.support.ProducedString;

@AroundConstructBinding
@ClientEndpoint
public class AnnotatedClient {

    public static boolean postConstructCalled = false;

    public static boolean injectionOK = false;

    private static String name;

    private static final BlockingDeque<String> queue = new LinkedBlockingDeque<>();

    @Inject
    private Alpha alpha;

    @Inject
    public AnnotatedClient(@ProducedString String name) {
        AnnotatedClient.name = name + "#AnnotatedClient";
    }

    @Inject
    public void setBravo(Bravo bravo) {
        injectionOK = (alpha != null) && (bravo != null) && (name != null);
    }

    @PostConstruct
    private void init() {
        postConstructCalled = true;
    }

    @OnOpen
    @ComponentInterceptorBinding
    public void open(final Session session) throws IOException {
        session.getBasicRemote().sendText("Hello");
    }

    @OnMessage
    @Interceptors(OnMessageClientInterceptor.class)
    public void message(final String message) {
        queue.add(message);
    }

    public static String getMessage() throws InterruptedException {
        return queue.poll(5, TimeUnit.SECONDS);
    }

    public static String getName() {
        return name;
    }

    public static void reset() {
        queue.clear();
        postConstructCalled = false;
        injectionOK = false;
        AnnotatedClient.name = null;
    }
}
