/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.session;

import org.jboss.as.web.session.SessionIdentifierCodec;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

/**
 * {@link SessionConfig} decorator that performs encoding/decoding of the session identifier.
 * In this way, routing is completely opaque to the request, session, and session manager.
 * @author Paul Ferraro
 */
public class CodecSessionConfig implements SessionConfig {

    private final SessionConfig config;
    private final SessionIdentifierCodec codec;

    public CodecSessionConfig(SessionConfig config, SessionIdentifierCodec codec) {
        this.config = config;
        this.codec = codec;
    }

    @Override
    public void setSessionId(HttpServerExchange exchange, String sessionId) {
        CharSequence encodedSessionId = this.codec.encode(sessionId);
        String requestedSessionId = this.config.findSessionId(exchange);
        // Apply only if identifier changed
        if (!encodedSessionId.equals(requestedSessionId)) {
            this.config.setSessionId(exchange, encodedSessionId.toString());
        }
    }

    @Override
    public void clearSession(HttpServerExchange exchange, String sessionId) {
        CharSequence encodedSessionId = this.codec.encode(sessionId);
        this.config.clearSession(exchange, encodedSessionId.toString());
    }

    @Override
    public String findSessionId(HttpServerExchange exchange) {
        String requestedSessionId = this.config.findSessionId(exchange);
        return (requestedSessionId != null) ? this.codec.decode(requestedSessionId).toString() : null;
    }

    @Override
    public SessionCookieSource sessionCookieSource(HttpServerExchange exchange) {
        return this.config.sessionCookieSource(exchange);
    }

    @Override
    public String rewriteUrl(String originalUrl, String sessionId) {
        CharSequence encodedSessionId = this.codec.encode(sessionId);
        return this.config.rewriteUrl(originalUrl, encodedSessionId.toString());
    }
}
