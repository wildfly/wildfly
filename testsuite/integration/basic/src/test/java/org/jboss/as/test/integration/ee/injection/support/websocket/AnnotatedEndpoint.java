/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.websocket.OnMessage;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.AroundConstructBinding;
import org.jboss.as.test.integration.ee.injection.support.Bravo;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptorBinding;
import org.jboss.as.test.integration.ee.injection.support.ProducedString;

@AroundConstructBinding
@ServerEndpoint("/websocket/{name}")
public class AnnotatedEndpoint {

    public static boolean postConstructCalled = false;

    public static boolean injectionOK = false;

    private static String name;

    @Inject
    private Alpha alpha;

    @Inject
    public AnnotatedEndpoint(@ProducedString String name) {
        AnnotatedEndpoint.name = name + "#AnnotatedEndpoint";
    }

    @Inject
    public void setBravo(Bravo bravo) {
        injectionOK = (alpha != null) && (bravo != null) && (name != null);
    }

    @PostConstruct
    private void init() {
        postConstructCalled = true;
    }

    @OnMessage
    @ComponentInterceptorBinding
    public String message(String message, @PathParam("name") String name) {
        return message + " " + name;
    }

    public static String getName() {
        return name;
    }

    public static void reset() {
        postConstructCalled = false;
        injectionOK = false;
        AnnotatedEndpoint.name = null;
    }
}
