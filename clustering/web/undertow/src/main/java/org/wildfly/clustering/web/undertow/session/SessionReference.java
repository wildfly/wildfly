/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Map;

import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.server.util.Reference;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

/**
 * A reference to an Undertow session, for use by {@link jakarta.servlet.http.HttpSession#getAccessor()}.
 * @author Paul Ferraro
 */
public class SessionReference implements Reference<UndertowSession> {

    private final Reader<UndertowSession> reader;

    public SessionReference(UndertowSessionManager manager, String id) {
        this.reader = manager.getSessionManager().getSessionReference(id).getReader().map(new Function<>() {
            @Override
            public UndertowSession apply(Session<Map<String, Object>> session) {
                if (session == null) {
                    // Per HttpSession.Accessor.access(...) specification
                    throw UndertowClusteringLogger.ROOT_LOGGER.sessionIsInvalid(id);
                }
                return new AbstractDistributableSession(manager, Reference.of(session)) {
                    @Override
                    public void requestDone(HttpServerExchange serverExchange) {
                        // Not relevant
                    }

                    @Override
                    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
                        // Not relevant
                        return this.getId();
                    }
                };
            }
        });
    }

    @Override
    public Reader<UndertowSession> getReader() {
        return this.reader;
    }
}
