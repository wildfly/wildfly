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
package org.wildfly.extension.undertow.security;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * The HTTP Handler that does proper security manager setup for a servlet invocation context.
 * @author Eduardo Martins
 */
public class WildflySecurityManagerPrivilegedHandler implements HttpHandler {

    private final HttpHandler next;

    public WildflySecurityManagerPrivilegedHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // if a security manager is set then invoke next handler in a privileged action through WildflySecurityManager, which will setup the invocation context if needed
        if(System.getSecurityManager() != null) {
            final PrivilegedExceptionAction<Void> action = new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    next.handleRequest(exchange);
                    return null;
                }
            };
            try {
                WildFlySecurityManager.doChecked(action);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        } else {
            next.handleRequest(exchange);
        }
    }

    /**
     *
     * @return
     */
    public static HandlerWrapper wrapper() {
        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                return new WildflySecurityManagerPrivilegedHandler(handler);
            }
        };
    }
}
