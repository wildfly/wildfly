/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.OptionalInt;

import org.wildfly.clustering.server.util.BlockingReference;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

/**
 * Session decorator that auto-detaches on {@link #requestDone(HttpServerExchange)}.
 * @author Paul Ferraro
 */
public class DetachableSession extends AbstractReferencedSession {

    private final BlockingReference<UndertowSession> reference;

    public DetachableSession(UndertowSession session) {
        this(session.getSessionManager(), BlockingReference.of(session));
    }

    private DetachableSession(UndertowSessionManager manager, BlockingReference<UndertowSession> reference) {
        super(manager, reference);
        this.reference = reference;
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        // Ideally, there would be no more session invocations after this method invokes.
        // Unfortunately, Undertow does not guarantee this.
        // Therefore, any subsequent session access should use a detached session
        this.reference.getWriter().getAndUpdate(this::detach).requestDone(exchange);
    }

    private UndertowSession detach(UndertowSession session) {
        // Workaround for Undertow 2.3.x, which uses exception handling to determine session validity
        OptionalInt maxInactiveInterval = session.isValid() ? OptionalInt.of(session.getMaxInactiveInterval()) : OptionalInt.empty();
        return new DetachedSession(session.getSessionManager(), session.getId()) {
            @Override
            public int getMaxInactiveInterval() {
                return maxInactiveInterval.orElseThrow(IllegalStateException::new);
            }
        };
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        return this.reference.getReader().map(session -> session.changeSessionId(exchange, config)).get();
    }
}
