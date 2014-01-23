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
package org.wildfly.clustering.web.undertow.session;

import org.wildfly.clustering.web.session.ImmutableSession;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;

/**
 * Undertow adapter for an {@link ImmutableSession}.
 * @author Paul Ferraro
 */
public class DistributableImmutableSession extends AbstractDistributableSession<ImmutableSession> {

    private final SessionManager manager;
    private final ImmutableSession session;

    public DistributableImmutableSession(SessionManager manager, ImmutableSession session) {
        this.manager = manager;
        this.session = session;
    }

    @Override
    public SessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    protected ImmutableSession getSession() {
        return this.session;
    }

    @Override
    public void requestDone(HttpServerExchange serverExchange) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object setAttribute(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object removeAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        throw new UnsupportedOperationException();
    }
}
