/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ee.injection.support.websocket;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

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
