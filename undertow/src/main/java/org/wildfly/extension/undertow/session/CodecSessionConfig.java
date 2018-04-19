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
import io.undertow.util.AttachmentKey;

/**
 * {@link SessionConfig} decorator that performs encoding/decoding of the session identifier.
 * In this way, routing is completely opaque to the request, session, and session manager.
 * @author Paul Ferraro
 */
public class CodecSessionConfig implements SessionConfig {

    private final SessionConfig config;
    private final SessionIdentifierCodec codec;
    private static final AttachmentKey<Boolean> SESSION_ID_SET = AttachmentKey.create(Boolean.class);

    public CodecSessionConfig(SessionConfig config, SessionIdentifierCodec codec) {
        this.config = config;
        this.codec = codec;
    }

    @Override
    public void setSessionId(HttpServerExchange exchange, String sessionId) {
        exchange.putAttachment(SESSION_ID_SET, Boolean.TRUE);
        this.config.setSessionId(exchange, this.codec.encode(sessionId));
    }

    @Override
    public void clearSession(HttpServerExchange exchange, String sessionId) {
        this.config.clearSession(exchange, this.codec.encode(sessionId));
    }

    @Override
    public String findSessionId(HttpServerExchange exchange) {
        String encodedSessionId = this.config.findSessionId(exchange);
        if (encodedSessionId == null) return null;
        String sessionId = this.codec.decode(encodedSessionId);
        // Check if the encoding for this session has changed
        String reencodedSessionId = this.codec.encode(sessionId);
        if (!reencodedSessionId.equals(encodedSessionId) && exchange.getAttachment(SESSION_ID_SET) == null) {
            this.config.setSessionId(exchange, reencodedSessionId);
        }
        return sessionId;
    }

    @Override
    public SessionCookieSource sessionCookieSource(HttpServerExchange exchange) {
        return this.config.sessionCookieSource(exchange);
    }

    @Override
    public String rewriteUrl(String originalUrl, String sessionId) {
        return this.config.rewriteUrl(originalUrl, this.codec.encode(sessionId));
    }
}
