/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.Set;
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
 * A {@link ServletRequestListener} that notifies a control point of request completion.
 * @author Paul Ferraro
 */
public class SuspendedServerRequestListener implements ServletRequestListener, UnaryOperator<DeploymentInfo>, ExchangeCompletionListener {
    static final Logger LOGGER = Logger.getLogger(SuspendedServerHandlerWrapper.class);

    private final ControlPoint entryPoint;

    public SuspendedServerRequestListener(ControlPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public DeploymentInfo apply(DeploymentInfo deployment) {
        deployment.addListener(new ListenerInfo(ServletRequestListener.class, new ImmediateInstanceFactory<>(this)));
        SessionManagerFactory factory = deployment.getSessionManagerFactory();
        if (factory != null) {
            // Decorate sessions with ControlPoint completion logic
            deployment.setSessionManagerFactory(new ControlPointSessionManagerFactory(factory, this.entryPoint));
        }
        return deployment;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        ServletRequestContext context = ServletRequestContext.requireCurrent();
        HttpServerExchange exchange = context.getExchange();
        if (exchange.removeAttachment(SuspendedServerHandlerWrapper.RUN_RESULT_KEY) == RunResult.RUN) {
            // If a session is associated with the request context, defer ControlPoint completion until Session#requestDone(...)
            // See: io.undertow.servlet.spec.HttpServletResponseImpl#responseDone()
            if (context.getSession() != null) {
                // If the exchange is not yet complete, defer request completion until exchange is complete
                if (exchange.isComplete()) {
                    this.entryPoint.requestComplete();
                    LOGGER.tracef("END request (via %s): %s", ServletRequestListener.class.getSimpleName(), exchange.getRequestURI());
                } else {
                    exchange.addExchangeCompleteListener(this);
                }
            }
        }
    }

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
        try {
            nextListener.proceed();
        } finally {
            // If a session is still associated with the exchange, defer ControlPoint completion until Session#requestDone(...)
            if (exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY).getSession() == null) {
                this.entryPoint.requestComplete();
                LOGGER.tracef("END request (via %s): %s", ExchangeCompletionListener.class.getSimpleName(), exchange.getRequestURI());
            }
        }
    }

    /**
     * Session manager factory decorator that creates ControlPoint-aware session managers.
     */
    static class ControlPointSessionManagerFactory implements SessionManagerFactory {

        private final SessionManagerFactory factory;
        private final ControlPoint controlPoint;

        ControlPointSessionManagerFactory(SessionManagerFactory factory, ControlPoint controlPoint) {
            this.factory = factory;
            this.controlPoint = controlPoint;
        }

        @Override
        public SessionManager createSessionManager(Deployment deployment) {
            return new ControlPointSessionManager(this.factory.createSessionManager(deployment), this.controlPoint);
        }
    }

    /**
     * Session manager decorator that creates ControlPoint-aware sessions.
     */
    static class ControlPointSessionManager implements SessionManager {
        private final SessionManager manager;
        private final ControlPoint controlPoint;

        ControlPointSessionManager(SessionManager manager, ControlPoint controlPoint) {
            this.manager = manager;
            this.controlPoint = controlPoint;
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
            return new ControlPointSession(this.manager.createSession(exchange, config), this.manager, this.controlPoint);
        }

        @Override
        public Session getSession(HttpServerExchange exchange, SessionConfig config) {
            Session session = this.manager.getSession(exchange, config);
            return (session != null) ? new ControlPointSession(session, this.manager, this.controlPoint) : null;
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
     * Session decorator that creates ControlPoint-aware sessions.
     */
    static class ControlPointSession implements Session {
        private final Session session;
        private final SessionManager manager;
        private final ControlPoint controlPoint;

        ControlPointSession(Session session, SessionManager manager, ControlPoint controlPoint) {
            this.session = session;
            this.manager = manager;
            this.controlPoint = controlPoint;
        }

        @Override
        public SessionManager getSessionManager() {
            return this.manager;
        }

        @Override
        public String getId() {
            return this.session.getId();
        }

        @Override
        public void requestDone(HttpServerExchange exchange) {
            try {
                this.session.requestDone(exchange);
            } finally {
                this.controlPoint.requestComplete();
                LOGGER.tracef("END request (via %s): %s", Session.class.getSimpleName(), exchange.getRequestURI());
            }
        }

        @Override
        public long getCreationTime() {
            return this.session.getCreationTime();
        }

        @Override
        public long getLastAccessedTime() {
            return this.session.getLastAccessedTime();
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
            this.session.setMaxInactiveInterval(interval);
        }

        @Override
        public int getMaxInactiveInterval() {
            return this.session.getMaxInactiveInterval();
        }

        @Override
        public Object getAttribute(String name) {
            return this.session.getAttribute(name);
        }

        @Override
        public Set<String> getAttributeNames() {
            return this.session.getAttributeNames();
        }

        @Override
        public Object setAttribute(String name, Object value) {
            return this.session.setAttribute(name, value);
        }

        @Override
        public Object removeAttribute(String name) {
            return this.session.removeAttribute(name);
        }

        @Override
        public void invalidate(HttpServerExchange exchange) {
            this.session.invalidate(exchange);
        }

        @Override
        public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
            return this.session.changeSessionId(exchange, config);
        }
    }
}
