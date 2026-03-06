/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.wildfly.clustering.function.BiConsumer;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.function.UnaryOperator;
import org.wildfly.clustering.server.util.Reference;
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
    private static final Function<Set<String>, Set<String>> COPY_SET = Set::copyOf;
    private static final Predicate<Session<Map<String, Object>>> VALID = Session::isValid;
    private static final UnaryOperator<Session<Map<String, Object>>> REQUIRE_VALID = UnaryOperator.when(VALID, UnaryOperator.identity(), UnaryOperator.of(Consumer.<Session<Map<String, Object>>>of().thenThrow(IllegalStateException::new), Supplier.of(null)));
    private static final Function<Session<Map<String, Object>>, String> IDENTIFIER = Session::getId;
    private static final Function<Session<Map<String, Object>>, Map<String, Object>> CONTEXT = REQUIRE_VALID.thenApply(Session::getContext);
    private static final Function<Session<Map<String, Object>>, SessionMetaData> METADATA = REQUIRE_VALID.thenApply(Session::getMetaData);
    private static final Function<Session<Map<String, Object>>, Map<String, Object>> ATTRIBUTES = REQUIRE_VALID.thenApply(Session::getAttributes);
    private static final Function<SessionMetaData, Instant> CREATION_TIME = SessionMetaData::getCreationTime;
    private static final Function<SessionMetaData, Optional<Instant>> LAST_ACCESS_TIME = SessionMetaData::getLastAccessStartTime;
    private static final Function<SessionMetaData, Map.Entry<Instant, Optional<Instant>>> CREATION_LAST_ACCESS_TIME = Function.entry(CREATION_TIME, LAST_ACCESS_TIME);
    private static final Function<SessionMetaData, Optional<Duration>> MAX_IDLE = SessionMetaData::getMaxIdle;
    private static final BiConsumer<SessionMetaData, Duration> SET_MAX_IDLE = SessionMetaData::setMaxIdle;
    private static final BiFunction<Map<String, Object>, String, Object> GET_ATTRIBUTE = Map::get;
    private static final BiFunction<Map<String, Object>, String, Object> REMOVE_ATTRIBUTE = Map::remove;
    private static final Function<Map<String, Object>, Set<String>> ATTRIBUTE_NAMES = Function.of(Map::keySet, COPY_SET);

    private final Reference.Reader<Session<Map<String, Object>>> sessionReader;
    private final Reference.Reader<SessionMetaData> sessionMetaDataReader;
    private final Reference.Reader<Map<String, Object>> sessionAttributesReader;
    private final Reference.Reader<Map<String, Object>> sessionContextReader;

    protected AbstractDistributableSession(UndertowSessionManager manager, Reference<Session<Map<String, Object>>> reference) {
        this(manager, reference.getReader());
    }

    private AbstractDistributableSession(UndertowSessionManager manager, Reference.Reader<Session<Map<String, Object>>> sessionReader) {
        super(manager, sessionReader.map(IDENTIFIER));
        this.sessionReader = sessionReader;
        this.sessionMetaDataReader = sessionReader.map(METADATA);
        this.sessionAttributesReader = sessionReader.map(ATTRIBUTES);
        this.sessionContextReader = sessionReader.map(CONTEXT);
    }

    @Override
    public boolean isValid() {
        return this.sessionReader.map(VALID.thenBox()).get();
    }

    @Override
    public long getCreationTime() {
        return this.sessionMetaDataReader.map(CREATION_TIME).get().toEpochMilli();
    }

    @Override
    public long getLastAccessedTime() {
        Map.Entry<Instant, Optional<Instant>> entry = this.sessionMetaDataReader.map(CREATION_LAST_ACCESS_TIME).get();
        return entry.getValue().orElse(entry.getKey()).toEpochMilli();
    }

    @Override
    public int getMaxInactiveInterval() {
        return (int) this.sessionMetaDataReader.map(MAX_IDLE).get().orElse(Duration.ZERO).getSeconds();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        Duration maxIdle = (interval > 0) ? Duration.ofSeconds(interval) : Duration.ZERO;
        this.sessionMetaDataReader.read(SET_MAX_IDLE.composeUnary(Function.identity(), Function.of(maxIdle)));
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.sessionAttributesReader.map(ATTRIBUTE_NAMES).get();
    }

    @Override
    public Object getAttribute(String name) {
        Function<Map<String, Object>, Object> operation = GET_ATTRIBUTE.composeUnary(Function.identity(), Function.of(name));
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
        Function<Map<String, Object>, Object> operation = attributes -> attributes.put(name, value);
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
        Function<Map<String, Object>, Object> operation = REMOVE_ATTRIBUTE.composeUnary(Function.identity(), Function.of(name));
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
        this.sessionReader.map(REQUIRE_VALID).read(session -> {
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

    /*
     * New method in io.undertow.server.session.Session that can add the @Override annotation when Undertow is upgraded
     */
    public io.undertow.server.session.Session detach() {
        return new DetachedSession(this.getSessionManager(), this.getId());
    }
}
