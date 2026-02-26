/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.security.PrivilegedAction;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

import io.undertow.predicate.Predicate;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.server.session.SessionManagerStatistics;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.SessionManagerFactory;
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.util.AttachmentKey;

import org.jboss.logging.Logger;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Configures a {@link DeploymentInfo} for use with a {@link ControlPoint}.
 * @author Paul Ferraro
 */
public class ControlPointDeploymentInfoConfigurator implements UnaryOperator<DeploymentInfo>, HandlerWrapper, ThreadSetupHandler, ExchangeCompletionListener {
    static final Logger LOGGER = Logger.getLogger(ControlPointDeploymentInfoConfigurator.class);
    static final AttachmentKey<RunResult> RUN_RESULT_KEY = AttachmentKey.create(RunResult.class);
    // Used to control invocations of Session.requestDone(...)
    static final AttachmentKey<Boolean> COMPLETE = AttachmentKey.create(Boolean.class);

    private final ControlPoint entryPoint;
    private final List<Predicate> allowSuspendedRequests;

    public ControlPointDeploymentInfoConfigurator(ControlPoint entryPoint, List<Predicate> allowSuspendedRequests) {
        this.entryPoint = entryPoint;
        this.allowSuspendedRequests = allowSuspendedRequests;
    }

    @Override
    public DeploymentInfo apply(DeploymentInfo deployment) {
        // ControlPoint.beginRequest() triggered within initial handler chain
        // ControlPoint.requestComplete() will be triggered by the later of 2 events: ExchangeCompletionListener or ThreadSetupHandler.Action
        // This complexity is needed to workaround a quirk in Undertow, where io.undertow.servlet.spec.HttpServletResponseImpl#responseDone() triggers Session.requestDone() even if request processing is not complete, e.g. application events have yet to be emitted.
        return deployment.addInitialHandlerChainWrapper(this)
                .addOuterHandlerChainWrapper(ControlPointRequestAttributeHandler::new)
                .addThreadSetupAction(this)
                .setSessionManagerFactory(Optional.ofNullable(deployment.getSessionManagerFactory()).map(CompletableSessionManagerFactory::new).orElse(null));
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        return exchange -> {
            RunResult result = this.entryPoint.beginRequest();
            if (exchange.putAttachment(RUN_RESULT_KEY, result) == RunResult.RUN) {
                // N.B. There should be no existing attachment, but if there is, complete it
                LOGGER.debugf("Request #%s auto-completing existing RunResult exchange attachment", exchange.getRequestId());
                this.entryPoint.requestComplete();
            }
            boolean accepted = result == RunResult.RUN;
            if (accepted) {
                LOGGER.tracef("Request #%s BEGIN", exchange.getRequestId());
                // Used to distinguish between mid-request and post-request exchange completion
                // Used to workaround unwanted post-ExchangeCompletionListener invocation of Session.requestDone(...)
                exchange.putAttachment(COMPLETE, Boolean.FALSE);
                // If accepted by ControlPoint, signal ControlPoint.requestComplete() when exchange completes
                exchange.addExchangeCompleteListener(this);
            }
            if (accepted || this.allowSuspendedRequest(exchange)) {
                handler.handleRequest(exchange);
            } else {
                LOGGER.debugf("Request #%s rejected due to server suspension", exchange.getRequestId());
                SuspendedServerHandler.DEFAULT.handleRequest(exchange);
            }
        };
    }

    @Override
    public <T, C> Action<T, C> create(Action<T, C> action) {
        return new Action<>() {
            @Override
            public T call(HttpServerExchange exchange, C context) throws Exception {
                try {
                    return action.call(exchange, context);
                } finally {
                    // If exchange completed within handler chain, ControlPoint.requestComplete() will have been deferred
                    if ((exchange != null) && exchange.isComplete() && (exchange.removeAttachment(ControlPointDeploymentInfoConfigurator.RUN_RESULT_KEY) == RunResult.RUN)) {
                        ControlPointDeploymentInfoConfigurator.this.complete(exchange);
                        LOGGER.tracef("Request #%s END via ThreadSetupHandler.Action", exchange.getRequestId());
                    }
                }
            }
        };
    }

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
        try {
            nextListener.proceed();
        } finally {
            // If exchange completes outside of handler chain, trigger ControlPoint.requestComplete() (if required)
            // Otherwise, defer completion signal until ThreadSetupHandler.Action tear-down.
            if (!exchange.isDispatched() && (exchange.removeAttachment(ControlPointDeploymentInfoConfigurator.RUN_RESULT_KEY) == RunResult.RUN)) {
                this.complete(exchange);
                LOGGER.tracef("Request #%s END via ExchangeCompleteListener", exchange.getRequestId());
            }
        }
    }

    private void complete(HttpServerExchange exchange) {
        exchange.putAttachment(COMPLETE, Boolean.TRUE);
        ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        try {
            if (context != null) {
                HttpSessionImpl session = context.getSession();
                // Undertow does not trigger requestDone(...) for invalid sessions.
                if ((session != null) && !session.isInvalid()) {
                    WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
                        @Override
                        public Void run() {
                            session.getSession().requestDone(exchange);
                            return null;
                        }
                    });
                }
            }
        } finally {
            this.entryPoint.requestComplete();
            // Reset attachment value to prevent unwanted Session.requestDone(...) invocation via HttpServletResponseImpl.responseDone()
            exchange.putAttachment(COMPLETE, Boolean.FALSE);
        }
    }

    boolean allowSuspendedRequest(HttpServerExchange exchange) {
        for (Predicate predicate : this.allowSuspendedRequests) {
            if (predicate.resolve(exchange)) {
                return true;
            }
        }
        return false;
    }

    static class ControlPointRequestAttributeHandler implements HttpHandler {
        private final HttpHandler next;

        ControlPointRequestAttributeHandler(HttpHandler next) {
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
            if ((context != null) && exchange.getAttachment(RUN_RESULT_KEY) == RunResult.REJECTED) {
                // If this request was rejected by the ControlPoint, we must have explicitly allowed it
                // Set request attribute to be read by other subsystems
                // TODO Replace internal references to this request attribute with something more robust
                context.getServletRequest().setAttribute("org.wildfly.suspended", "true");
            }
            this.next.handleRequest(exchange);
        }
    }

    /**
     * Session manager factory decorator whose created managers create sessions that will invoke {@link Session#requestDone(HttpServerExchange)} only when indicated.
     */
    static class CompletableSessionManagerFactory implements SessionManagerFactory {

        private final SessionManagerFactory factory;

        CompletableSessionManagerFactory(SessionManagerFactory factory) {
            this.factory = factory;
        }

        @Override
        public SessionManager createSessionManager(Deployment deployment) {
            return new CompletableSessionManager(this.factory.createSessionManager(deployment));
        }
    }

    /**
     * Session manager decorator that creates sessions that will invoke {@link Session#requestDone(HttpServerExchange)} only when indicated.
     */
    static class CompletableSessionManager implements SessionManager {
        private final SessionManager manager;

        CompletableSessionManager(SessionManager manager) {
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
            return new CompletableSession(this, this.manager.createSession(exchange, config));
        }

        @Override
        public Session getSession(HttpServerExchange exchange, SessionConfig config) {
            Session session = this.manager.getSession(exchange, config);
            return (session != null) ? this.decorate(session) : null;
        }

        @Override
        public Session getSession(String sessionId) {
            Session session = this.manager.getSession(sessionId);
            return (session != null) ? this.decorate(session) : null;
        }

        private Session decorate(Session session) {
            return session instanceof CompletableSession ? session : new CompletableSession(this, session);
        }

        @Override
        public void registerSessionListener(SessionListener listener) {
            this.manager.registerSessionListener(this.decorate(listener));
        }

        @Override
        public void removeSessionListener(SessionListener listener) {
            this.manager.removeSessionListener(this.decorate(listener));
        }

        private SessionListener decorate(SessionListener listener) {
            return new SessionListener() {
                @Override
                public void sessionCreated(Session session, HttpServerExchange exchange) {
                    listener.sessionCreated(CompletableSessionManager.this.decorate(session), exchange);
                }

                @Override
                public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
                    listener.sessionDestroyed(CompletableSessionManager.this.decorate(session), exchange, reason);
                }

                @Override
                public void attributeAdded(Session session, String name, Object value) {
                    listener.attributeAdded(CompletableSessionManager.this.decorate(session), name, value);
                }

                @Override
                public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
                    listener.attributeUpdated(CompletableSessionManager.this.decorate(session), name, newValue, oldValue);
                }

                @Override
                public void attributeRemoved(Session session, String name, Object oldValue) {
                    listener.attributeRemoved(CompletableSessionManager.this.decorate(session), name, oldValue);
                }

                @Override
                public void sessionIdChanged(Session session, String oldSessionId) {
                    listener.sessionIdChanged(CompletableSessionManager.this.decorate(session), oldSessionId);
                }

                @Override
                public boolean equals(Object value) {
                    return listener.equals(value);
                }

                @Override
                public int hashCode() {
                    return listener.hashCode();
                }

                @Override
                public String toString() {
                    return listener.toString();
                }
            };
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
    static class CompletableSession implements Session {
        private final SessionManager manager;
        private final Session session;
        // TODO Remove following Undertow 2.4 upgrade
        private final AtomicBoolean valid = new AtomicBoolean(true);
        private volatile int maxInactiveInterval;

        CompletableSession(SessionManager manager, Session session) {
            this.manager = manager;
            this.session = session;
            this.maxInactiveInterval = session.getMaxInactiveInterval();
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
            if (exchange.getAttachment(COMPLETE) != Boolean.FALSE) {
                this.session.requestDone(exchange);
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
            this.maxInactiveInterval = interval;
        }

        @Override
        public int getMaxInactiveInterval() {
            // TODO Replace with direct delegation following Undertow upgrade.
            // return this.session.getMaxInactiveInterval();
            if (this.isInvalid()) {
                // As required by HttpSessionImpl.isInvalid()
                throw new IllegalStateException();
            }
            return this.maxInactiveInterval;
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
            this.valid.set(false);
            this.session.invalidate(exchange);
        }

        @Override
        public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
            return this.session.changeSessionId(exchange, config);
        }

        @Override
        public boolean isInvalid() {
            // TODO Delegate to new method following Undertow upgrade.
            // return this.session.isInvalid();
            return !this.valid.get();
        }

        /*
         * New method in io.undertow.server.session.Session that can add the @Override annotation when Undertow is upgraded
         */
        public io.undertow.server.session.Session detach() {
            // TODO Delegate to new method following Undertow upgrade.
            // return this.session.detach();
            return this.manager.getSession(this.session.getId());
        }
    }
}
