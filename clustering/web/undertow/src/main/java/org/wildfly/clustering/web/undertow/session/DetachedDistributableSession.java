/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Map;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.server.session.SessionListeners;

import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.session.Session;

/**
 * A detached session, for use with {@link SessionManager#getSession(String)}.
 * @author Paul Ferraro
 */
public class DetachedDistributableSession extends AbstractSession {
    private final RecordableSessionManagerStatistics statistics;

    public DetachedDistributableSession(UndertowSessionManager manager, Session<Map<String, Object>> session, RecordableSessionManagerStatistics statistics) {
        super(manager, Supplier.of(session));
        this.statistics = statistics;
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        Session<Map<String, Object>> session = super.get();
        if (session.isValid()) {
            SessionListeners listeners = this.getSessionManager().getSessionListeners();
            listeners.sessionDestroyed(this, exchange, SessionDestroyedReason.INVALIDATED);

            for (Map.Entry<String, Object> attributesEntry : session.getAttributes().entrySet()) {
                listeners.attributeRemoved(this, attributesEntry.getKey(), attributesEntry.getValue());
            }
            if (this.statistics != null) {
                this.statistics.getInactiveSessionRecorder().record(session.getMetaData());
            }
        }
        super.invalidate(exchange);
    }

    @Override
    public void requestDone(HttpServerExchange serverExchange) {
        // Do nothing
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        // A detached session cannot change its ID
        return this.getId();
    }

    @Override
    public boolean isInvalid() {
        return super.get().isValid();
    }
}
