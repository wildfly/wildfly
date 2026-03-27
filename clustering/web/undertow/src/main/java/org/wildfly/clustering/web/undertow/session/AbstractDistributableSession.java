/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.util.Reference;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.ImmutableSessionMetaData;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionMetaData;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
import jakarta.servlet.http.HttpServletRequest;

/**
 * An Undertow session that delegates to a {@link org.wildfly.clustering.session.Session} reference.
 * @author Paul Ferraro
 */
public abstract class AbstractDistributableSession extends AbstractSession {
    // These mechanisms can auto-reauthenticate and thus use local context (instead of replicating)
    private static final Set<String> AUTO_REAUTHENTICATING_MECHANISMS = Set.of(HttpServletRequest.BASIC_AUTH, HttpServletRequest.DIGEST_AUTH, HttpServletRequest.CLIENT_CERT_AUTH);
    private static final Set<String> LOCAL_CONTEXT_ATTRIBUTES = Set.of(WEB_SOCKET_CHANNELS_ATTRIBUTE);
    private static final Function<Session<Map<String, Object>>, Map<String, Object>> CONTEXT = Function.when(ImmutableSession.VALID, Session::getContext, Function.of(Consumer.of().thenThrow(IllegalStateException::new), Supplier.of(null)));

    private final Reference.Reader<Session<Map<String, Object>>> sessionReader;
    private final Reference.Reader<SessionMetaData> sessionMetaDataReader;
    private final Reference.Reader<Map<String, Object>> sessionAttributesReader;
    private final Reference.Reader<Map<String, Object>> sessionContextReader;

    protected AbstractDistributableSession(UndertowSessionManager manager, Reference<Session<Map<String, Object>>> reference) {
        this(manager, reference.getReader());
    }

    private AbstractDistributableSession(UndertowSessionManager manager, Reference.Reader<Session<Map<String, Object>>> sessionReader) {
        super(manager, sessionReader.map(ImmutableSession.IDENTIFIER));
        this.sessionReader = sessionReader;
        this.sessionMetaDataReader = sessionReader.map(Session.METADATA);
        this.sessionAttributesReader = sessionReader.map(Session.ATTRIBUTES);
        this.sessionContextReader = sessionReader.map(CONTEXT);
    }

    @Override
    public boolean isValid() {
        return this.sessionReader.map(ImmutableSession.VALID.thenBox()).get();
    }

    @Override
    public long getCreationTime() {
        return this.sessionMetaDataReader.map(ImmutableSessionMetaData.CREATION_TIME).get().toEpochMilli();
    }

    @Override
    public long getLastAccessedTime() {
        return this.sessionMetaDataReader.map(ImmutableSessionMetaData.LAST_ACCESS_TIME).get().toEpochMilli();
    }

    @Override
    public int getMaxInactiveInterval() {
        return (int) this.sessionMetaDataReader.map(ImmutableSessionMetaData.MAX_IDLE).get().orElse(Duration.ZERO).getSeconds();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        Duration maxIdle = (interval > 0) ? Duration.ofSeconds(interval) : Duration.ZERO;
        this.sessionMetaDataReader.read(SessionMetaData.MAX_IDLE.composeUnary(Function.identity(), Function.of(maxIdle)));
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.sessionAttributesReader.map(ImmutableSession.ATTRIBUTE_NAMES).get();
    }

    @Override
    public Object getAttribute(String name) {
        Function<Map<String, Object>, Object> operation = ImmutableSession.GET_ATTRIBUTE.composeUnary(Function.identity(), Function.of(name));
        if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
            Object auth = this.sessionContextReader.map(operation).get();
            return (auth != null) ? auth : this.sessionAttributesReader.map(operation).get();
        }
        if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
            return this.sessionContextReader.map(operation).get();
        }
        return this.sessionAttributesReader.map(operation).get();
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) return this.removeAttribute(name);
        Function<Map<String, Object>, Object> operation = Session.SET_ATTRIBUTE.composeUnary(Function.identity(), Function.of(Map.entry(name, value)));
        if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
            AuthenticatedSession auth = (AuthenticatedSession) value;
            Reference.Reader<Map<String, Object>> authReader = AUTO_REAUTHENTICATING_MECHANISMS.contains(auth.getMechanism()) ? this.sessionContextReader : this.sessionAttributesReader;
            return authReader.map(operation).get();
        }
        if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
            return this.sessionContextReader.map(operation).get();
        }
        Object old = this.sessionAttributesReader.map(operation).get();
        if (old == null) {
            this.getSessionManager().getSessionListeners().attributeAdded(AbstractDistributableSession.this, name, value);
        } else if (old != value) {
            this.getSessionManager().getSessionListeners().attributeUpdated(AbstractDistributableSession.this, name, value, old);
        }
        return old;
    }

    @Override
    public Object removeAttribute(String name) {
        Function<Map<String, Object>, Object> operation = Session.REMOVE_ATTRIBUTE.composeUnary(Function.identity(), Function.of(name));
        if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
            Object auth = this.sessionAttributesReader.map(operation).get();
            return (auth != null) ? auth : this.sessionContextReader.map(operation).get();
        }
        if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
            return this.sessionContextReader.map(operation).get();
        }
        Object old = this.sessionAttributesReader.map(operation).get();
        if (old != null) {
            this.getSessionManager().getSessionListeners().attributeRemoved(this, name, old);
        }
        return old;
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        this.sessionReader.map(Session.REQUIRE_VALID).read(session -> {
            // Trigger attribute listeners
            this.getSessionManager().getSessionListeners().sessionDestroyed(this, exchange, SessionDestroyedReason.INVALIDATED);

            for (Map.Entry<String, Object> attributesEntry : session.getAttributes().entrySet()) {
                this.getSessionManager().getSessionListeners().attributeRemoved(this, attributesEntry.getKey(), attributesEntry.getValue());
            }
            if (this.getSessionManager().getStatistics() != null) {
                this.getSessionManager().getStatistics().getInactiveSessionRecorder().record(session.getMetaData());
            }
            try {
                session.invalidate();
            } finally {
                if (exchange != null) {
                    SessionConfig config = exchange.getAttachment(SessionConfig.ATTACHMENT_KEY);
                    if (config != null) {
                        config.clearSession(exchange, session.getId());
                    }
                }
            }
        });
    }
}
