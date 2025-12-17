/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.server.session.SessionManagerStatistics;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.SessionManagerFactory;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import org.jboss.logging.Logger;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

/**
 * A {@link ServletRequestListener} that notifies a control point of request completion in the event that the exchange completed within the context of a servlet request.
 * @author Paul Ferraro
 */
public class ServletRequestCompletionListener implements ServletRequestListener, UnaryOperator<DeploymentInfo> {
    static final Logger LOGGER = Logger.getLogger(ServletRequestCompletionListener.class);

    private final ControlPoint entryPoint;

    public ServletRequestCompletionListener(ControlPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public DeploymentInfo apply(DeploymentInfo deployment) {
        // Registered first
        deployment.addListener(new ListenerInfo(ServletRequestListener.class, new ImmediateInstanceFactory<>(this)));
        SessionManagerFactory factory = deployment.getSessionManagerFactory();
        if (factory != null) {
            // Decorate sessions so that Session.requestDone(...) is called when the exchange completes
            // This effectively overrides logic within {@link io.undertow.servlet.spec.HttpServletResponseImpl#responseDone()}
            // and ensures that Session lifecycle does extend beyond request boundary, as determined by the ControlPoint.
            deployment.setSessionManagerFactory(new HttpServerExchangeSessionManagerFactory(factory));
        }
        return deployment;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        ServletRequestContext context = ServletRequestContext.requireCurrent();
        HttpServerExchange exchange = context.getExchange();
        // If this event was triggered after the exchange completed, ControlPoint.requestComplete() will not yet have been signalled
        // Otherwise, this will happen when the exchange completes
        if (exchange.isComplete() && (exchange.removeAttachment(ControlPointHandlerWrapper.RUN_RESULT_KEY) == RunResult.RUN)) {
            this.entryPoint.requestComplete();
            LOGGER.tracef("END request (via %s): %s", ServletRequestListener.class.getSimpleName(), exchange.getRequestURI());
        }
    }

    /**
     * Session manager factory decorator whose created managers create sessions whose lifecycles are bound by their {@link HttpServerExchange}.
     */
    static class HttpServerExchangeSessionManagerFactory implements SessionManagerFactory {

        private final SessionManagerFactory factory;

        HttpServerExchangeSessionManagerFactory(SessionManagerFactory factory) {
            this.factory = factory;
        }

        @Override
        public SessionManager createSessionManager(Deployment deployment) {
            return new HttpServerExchangeSessionManager(this.factory.createSessionManager(deployment));
        }
    }

    /**
     * Session manager decorator that creates sessions whose lifecycles are bound by their {@link HttpServerExchange}.
     */
    static class HttpServerExchangeSessionManager implements SessionManager {
        private final SessionManager manager;

        HttpServerExchangeSessionManager(SessionManager manager) {
            this.manager = manager;
        }

        @Override
        public String getDeploymentName() {
            return this.manager.getDeploymentName();
        }

        @Override
        public void start() {
            this.manager.start();
        }

        @Override
        public void stop() {
            this.manager.stop();
        }

        @Override
        public Session createSession(HttpServerExchange exchange, SessionConfig config) {
            return !exchange.isComplete() ? this.decorate(exchange, this.manager.createSession(exchange, config)) : new ExchangeCompletedSession(this, "");
        }

        @Override
        public Session getSession(HttpServerExchange exchange, SessionConfig config) {
            Session session = !exchange.isComplete() ? this.manager.getSession(exchange, config) : null;
            return (session != null) ? this.decorate(exchange, session) : null;
        }

        private Session decorate(HttpServerExchange exchange, Session session) {
            AtomicReference<Session> reference = new AtomicReference<>(session);
            exchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
                @Override
                public void exchangeEvent(HttpServerExchange exchange, NextListener next) {
                    try {
                        // Exchange is complete, so trigger request completion logic for the Session
                        session.requestDone(exchange);
                        // Neutralise Undertow's post-response handling of Session
                        reference.set(new ExchangeCompletedSession(HttpServerExchangeSessionManager.this, session.getId()));
                    } finally {
                        next.proceed();
                    }
                }
            });
            return new HttpServerExchangeSession(this.manager, reference::get);
        }

        @Override
        public Session getSession(String sessionId) {
            return this.manager.getSession(sessionId);
        }

        @Override
        public void registerSessionListener(SessionListener listener) {
            this.manager.registerSessionListener(listener);
        }

        @Override
        public void removeSessionListener(SessionListener listener) {
            this.manager.removeSessionListener(listener);
        }

        @Override
        public void setDefaultSessionTimeout(int timeout) {
            this.manager.setDefaultSessionTimeout(timeout);
        }

        @Override
        public Set<String> getTransientSessions() {
            return this.manager.getTransientSessions();
        }

        @Override
        public Set<String> getActiveSessions() {
            return this.manager.getActiveSessions();
        }

        @Override
        public Set<String> getAllSessions() {
            return this.manager.getAllSessions();
        }

        @Override
        public SessionManagerStatistics getStatistics() {
            return this.manager.getStatistics();
        }
    }

    /**
     * Session decorator whose lifecycle is bound by the {@link HttpServerExchange}.
     */
    static class HttpServerExchangeSession implements Session {
        private final Supplier<Session> session;
        private final SessionManager manager;
        // TODO Remove following Undertow upgrade
        private volatile boolean valid = true;

        HttpServerExchangeSession(SessionManager manager, Supplier<Session> session) {
            this.manager = manager;
            this.session = session;
        }

        @Override
        public SessionManager getSessionManager() {
            return this.manager;
        }

        @Override
        public String getId() {
            return this.session.get().getId();
        }

        @Override
        public void requestDone(HttpServerExchange exchange) {
            // Undertow otherwise detaches session from exchange following invalidation
            // Do not delegate if session is invalid
            if (!this.isInvalid()) {
                this.session.get().requestDone(exchange);
            }
        }

        @Override
        public long getCreationTime() {
            return this.session.get().getCreationTime();
        }

        @Override
        public long getLastAccessedTime() {
            return this.session.get().getLastAccessedTime();
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
            this.session.get().setMaxInactiveInterval(interval);
        }

        @Override
        public int getMaxInactiveInterval() {
            return this.session.get().getMaxInactiveInterval();
        }

        @Override
        public Object getAttribute(String name) {
            return this.session.get().getAttribute(name);
        }

        @Override
        public Set<String> getAttributeNames() {
            return this.session.get().getAttributeNames();
        }

        @Override
        public Object setAttribute(String name, Object value) {
            return this.session.get().setAttribute(name, value);
        }

        @Override
        public Object removeAttribute(String name) {
            return this.session.get().removeAttribute(name);
        }

        @Override
        public void invalidate(HttpServerExchange exchange) {
            this.valid = false;
            this.session.get().invalidate(exchange);
        }

        @Override
        public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
            return this.session.get().changeSessionId(exchange, config);
        }

        /*
         * New method in io.undertow.server.session.Session that can add the @Override annotation when Undertow is upgraded
         */
        public boolean isInvalid() {
            // TODO Delegate to new method following Undertow upgrade.
            // return this.session.get().isInvalid();
            return !this.valid;
        }

        /*
         * New method in io.undertow.server.session.Session that can add the @Override annotation when Undertow is upgraded
         */
        public io.undertow.server.session.Session detach() {
            // TODO Delegate to new method following Undertow upgrade.
            // return this.session.get().detach();
            return this.manager.getSession(this.getId());
        }
    }

    /**
     * A "completed" session substitution for use by {@link io.undertow.servlet.spec.HttpServletResponseImpl#responseDone()}.
     * N.B. This is not visible to deployments, but is only referenced via Undertow during post-request handling.
     */
    static class ExchangeCompletedSession implements Session {
        private final SessionManager manager;
        private final String id;

        ExchangeCompletedSession(SessionManager manager, String id) {
            this.id = id;
            this.manager = manager;
        }

        @Override
        public SessionManager getSessionManager() {
            return this.manager;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public void requestDone(HttpServerExchange exchange) {
        }

        @Override
        public long getCreationTime() {
            return 0L;
        }

        @Override
        public long getLastAccessedTime() {
            return 0L;
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
        }

        @Override
        public int getMaxInactiveInterval() {
            return 0;
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public Set<String> getAttributeNames() {
            return Set.of();
        }

        @Override
        public Object setAttribute(String name, Object value) {
            return null;
        }

        @Override
        public Object removeAttribute(String name) {
            return null;
        }

        @Override
        public void invalidate(HttpServerExchange exchange) {
        }

        @Override
        public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
            return this.id;
        }

        /*
         * New method in io.undertow.server.session.Session that can add the @Override annotation when Undertow is upgraded
         */
        public boolean isInvalid() {
            return false;
        }

        /*
         * New method in io.undertow.server.session.Session that can add the @Override annotation when Undertow is upgraded
         */
        public io.undertow.server.session.Session detach() {
            return this.manager.getSession(this.id);
        }
    }
}
