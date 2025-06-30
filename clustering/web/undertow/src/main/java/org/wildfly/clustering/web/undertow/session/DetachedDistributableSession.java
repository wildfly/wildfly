/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2025, Red Hat, Inc., and individual contributors
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
}
