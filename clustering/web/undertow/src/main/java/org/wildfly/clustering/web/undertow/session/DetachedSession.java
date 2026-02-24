/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

/**
 * An Undertow session that delegates all methods to a session reference.
 * For use outside of request scope.
 * @author Paul Ferraro
 */
public class DetachedSession extends AbstractReferencedSession {
    private final String id;

    public DetachedSession(UndertowSessionManager manager, String id) {
        super(manager, new DistributableSessionReference(manager, id));
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void requestDone(HttpServerExchange serverExchange) {
        // Not relevant to a detached session
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        // A detached session cannot change its identifier
        return this.id;
    }
}
