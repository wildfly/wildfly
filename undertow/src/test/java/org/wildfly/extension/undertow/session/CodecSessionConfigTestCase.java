/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

import org.jboss.as.web.session.SessionIdentifierCodec;
import org.junit.Test;

/**
 * Unit test for {@link CodecSessionConfig}
 * @author Paul Ferraro
 */
public class CodecSessionConfigTestCase {

    private final SessionConfig config = mock(SessionConfig.class);
    private final SessionIdentifierCodec codec = mock(SessionIdentifierCodec.class);

    private final SessionConfig subject = new CodecSessionConfig(this.config, this.codec);

    @Test
    public void findSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);

        when(this.config.findSessionId(exchange)).thenReturn(null);

        String result = this.subject.findSessionId(exchange);

        assertNull(result);

        String encodedSessionId = "session.route1";
        String sessionId = "session";

        when(this.config.findSessionId(exchange)).thenReturn(encodedSessionId);
        when(this.codec.decode(encodedSessionId)).thenReturn(sessionId);
        when(this.codec.encode(sessionId)).thenReturn(encodedSessionId);

        result = this.subject.findSessionId(exchange);

        assertSame(sessionId, result);
    }

    @Test
    public void setSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        String encodedSessionId = "session.route1";
        String sessionId = "session";

        when(this.config.findSessionId(exchange)).thenReturn(null);
        when(this.codec.encode(sessionId)).thenReturn(encodedSessionId);

        // Validate new session
        this.subject.setSessionId(exchange, sessionId);

        verify(this.config).setSessionId(exchange, encodedSessionId);

        reset(this.config);

        // Validate existing session
        when(this.config.findSessionId(exchange)).thenReturn(encodedSessionId);

        this.subject.setSessionId(exchange, sessionId);

        verify(this.config, never()).setSessionId(exchange, encodedSessionId);

        reset(this.config);

        // Validate failover request for existing session
        when(this.config.findSessionId(exchange)).thenReturn("session.route2");

        this.subject.setSessionId(exchange, sessionId);

        verify(this.config).setSessionId(exchange, encodedSessionId);
    }

    @Test
    public void clearSession() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        String encodedSessionId = "session.route";
        String sessionId = "session";

        when(this.codec.encode(sessionId)).thenReturn(encodedSessionId);

        this.subject.clearSession(exchange, sessionId);

        verify(this.config).clearSession(exchange, encodedSessionId);
    }

    @Test
    public void rewriteUrl() {
        String url = "http://test";
        String encodedUrl = "http://test/session";
        String encodedSessionId = "session.route";
        String sessionId = "session";

        when(this.codec.encode(sessionId)).thenReturn(encodedSessionId);
        when(this.config.rewriteUrl(url, encodedSessionId)).thenReturn(encodedUrl);

        String result = this.subject.rewriteUrl(url, sessionId);

        assertSame(encodedUrl, result);
    }

    @Test
    public void sessionCookieSource() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig.SessionCookieSource expected = SessionConfig.SessionCookieSource.OTHER;

        when(this.config.sessionCookieSource(exchange)).thenReturn(expected);

        SessionConfig.SessionCookieSource result = this.subject.sessionCookieSource(exchange);

        assertSame(expected, result);
    }
}
