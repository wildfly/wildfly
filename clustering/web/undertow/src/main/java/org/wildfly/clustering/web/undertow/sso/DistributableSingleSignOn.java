/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.undertow.sso;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;
import io.undertow.security.impl.SingleSignOn;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

/**
 * Adapts an {@link SSO} to a {@link SingleSignOn}.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOn implements InvalidatableSingleSignOn {

    private final SSO<AuthenticatedSession, String, Void> sso;
    private final SessionManagerRegistry registry;
    private final Batcher<Batch> batcher;
    private final Batch batch;
    private final SessionListener listener;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public DistributableSingleSignOn(SSO<AuthenticatedSession, String, Void> sso, SessionManagerRegistry registry, Batcher<Batch> batcher, Batch batch) {
        this.sso = sso;
        this.registry = registry;
        this.batcher = batcher;
        this.batch = batch;
        this.listener = new SessionIdChangeListener(sso, batcher, batch, this.closed);
    }

    @Override
    public String getId() {
        return this.sso.getId();
    }

    @Override
    public Account getAccount() {
        try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
            return this.sso.getAuthentication().getAccount();
        }
    }

    @Override
    public String getMechanismName() {
        try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
            return this.sso.getAuthentication().getMechanism();
        }
    }

    @Override
    public Iterator<Session> iterator() {
        try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
            Sessions<String> sessions = this.sso.getSessions();
            Set<String> deployments = sessions.getDeployments();
            List<Session> result = new ArrayList<>(deployments.size());
            for (String deployment : sessions.getDeployments()) {
                String sessionId = sessions.getSession(deployment);
                if (sessionId != null) {
                    SessionManager manager = this.registry.getSessionManager(deployment);
                    if (manager != null) {
                        result.add(new InvalidatableSession(manager, sessionId));
                    }
                }
            }
            return result.iterator();
        }
    }

    @Override
    public boolean contains(Session session) {
        try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
            return this.sso.getSessions().getDeployments().contains(session.getSessionManager().getDeploymentName());
        }
    }

    @Override
    public void add(Session session) {
        try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
            this.sso.getSessions().addSession(session.getSessionManager().getDeploymentName(), session.getId());
        }
        session.getSessionManager().registerSessionListener(this.listener);
    }

    @Override
    public void remove(Session session) {
        try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
            this.sso.getSessions().removeSession(session.getSessionManager().getDeploymentName());
        }
        session.getSessionManager().removeSessionListener(this.listener);
    }

    @Override
    public Session getSession(SessionManager manager) {
        try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
            String sessionId = this.sso.getSessions().getSession(manager.getDeploymentName());
            return (sessionId != null) ? new InvalidatableSession(manager, sessionId) : null;
        }
    }

    @Override
    public void close() {
        if (this.closed.compareAndSet(false, true)) {
            try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
                this.batch.close();
            }
        }
    }

    @Override
    public void invalidate() {
        // The batch associated with this SSO might not be valid (e.g. in the case of logout).
        try (BatchContext context = this.closed.compareAndSet(false, true) ? this.batcher.resumeBatch(this.batch) : null) {
            try (Batch batch = (context != null) ? this.batch : this.batcher.createBatch()) {
                this.sso.invalidate();
            }
        }
    }

    private static class InvalidatableSession implements Session {
        private final SessionManager manager;
        private final String sessionId;

        InvalidatableSession(SessionManager manager, String sessionId) {
            this.manager = manager;
            this.sessionId = sessionId;
        }

        @Override
        public String getId() {
            return this.sessionId;
        }

        @Override
        public SessionManager getSessionManager() {
            return this.manager;
        }

        @Override
        public void invalidate(HttpServerExchange exchange) {
            Session session = this.manager.getSession(exchange, new SimpleSessionConfig(this.sessionId));
            if (session != null) {
                session.invalidate(exchange);
            }
        }

        @Override
        public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> getAttributeNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getCreationTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLastAccessedTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getMaxInactiveInterval() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object removeAttribute(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void requestDone(HttpServerExchange exchange) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object setAttribute(String name, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
            throw new UnsupportedOperationException();
        }
    }

    private static class SimpleSessionConfig implements SessionConfig {
        private final String id;

        SimpleSessionConfig(String id) {
            this.id = id;
        }

        @Override
        public String findSessionId(HttpServerExchange exchange) {
            return this.id;
        }

        @Override
        public void setSessionId(HttpServerExchange exchange, String sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearSession(HttpServerExchange exchange, String sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SessionCookieSource sessionCookieSource(HttpServerExchange exchange) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String rewriteUrl(String originalUrl, String sessionId) {
            throw new UnsupportedOperationException();
        }
    }

    private static class SessionIdChangeListener implements SessionListener {
        private final SSO<AuthenticatedSession, String, Void> sso;
        private final Batcher<Batch> batcher;
        private final Batch batch;
        private final AtomicBoolean closed;

        SessionIdChangeListener(SSO<AuthenticatedSession, String, Void> sso, Batcher<Batch> batcher, Batch batch, AtomicBoolean closed) {
            this.sso = sso;
            this.batcher = batcher;
            this.batch = batch;
            this.closed = closed;
        }

        @Override
        public void sessionIdChanged(Session session, String oldSessionId) {
            // The batch associated with this SSO might not be valid in this context.
            try (BatchContext context = this.closed.compareAndSet(false, true) ? this.batcher.resumeBatch(this.batch) : null) {
                try (Batch batch = (context != null) ? this.batch : this.batcher.createBatch()) {
                    String deployment = session.getSessionManager().getDeploymentName();
                    Sessions<String> sessions = this.sso.getSessions();
                    sessions.removeSession(deployment);
                    sessions.addSession(deployment, session.getId());
                }
            }
        }

        @Override
        public void attributeAdded(Session session, String name, Object value) {
        }

        @Override
        public void attributeRemoved(Session session, String name, Object value) {
        }

        @Override
        public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
        }

        @Override
        public void sessionCreated(Session session, HttpServerExchange exchange) {
        }

        @Override
        public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
        }
    }
}
