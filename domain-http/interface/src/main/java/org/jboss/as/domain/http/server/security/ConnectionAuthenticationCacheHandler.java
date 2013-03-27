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
package org.jboss.as.domain.http.server.security;

import io.undertow.security.api.AuthenticatedSessionManager;
import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.api.SecurityNotification;
import io.undertow.security.api.SecurityNotification.EventType;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerConnection;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

/**
 * A temporary {@link HttpHandler} to cache the currently authenticated user against the connection.
 * <p/>
 * Longer term this will be eliminated but for now this is just to match the existing behaviour and minimise hitting the
 * CallbackHandlers.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConnectionAuthenticationCacheHandler implements HttpHandler {

    AttachmentKey<AuthenticatedSession> SESSION_KEY = AttachmentKey.create(AuthenticatedSession.class);

    private final NotificationReceiver NOTIFICATION_HANDLER = new SecurityNotificationHandler();
    private final AuthenticatedSessionManager SESSION_MANAGER = new DigestAuthenticatedSessionManager();

    private final HttpHandler next;

    public ConnectionAuthenticationCacheHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        SecurityContext securityContext = exchange.getAttachment(SecurityContext.ATTACHMENT_KEY);
        securityContext.registerNotificationReceiver(NOTIFICATION_HANDLER);
        exchange.putAttachment(AuthenticatedSessionManager.ATTACHMENT_KEY, SESSION_MANAGER);

        next.handleRequest(exchange);
    }

    private class SecurityNotificationHandler implements NotificationReceiver {

        @Override
        public void handleNotification(SecurityNotification notification) {
            EventType eventType = notification.getEventType();
            switch (eventType) {
                case AUTHENTICATED: {
                    HttpServerConnection connection = notification.getExchange().getConnection();
                    connection.putAttachment(SESSION_KEY,
                            new AuthenticatedSession(notification.getAccount(), notification.getMechanism()));
                    break;
                }
                case LOGGED_OUT: {
                    HttpServerConnection connection = notification.getExchange().getConnection();
                    connection.removeAttachment(SESSION_KEY);
                    break;
                }
            }
        }
    }

    private class DigestAuthenticatedSessionManager implements AuthenticatedSessionManager {

        @Override
        public AuthenticatedSession lookupSession(HttpServerExchange exchange) {
            HttpServerConnection connection = exchange.getConnection();

            return connection.getAttachment(SESSION_KEY);
        }

    }

}
