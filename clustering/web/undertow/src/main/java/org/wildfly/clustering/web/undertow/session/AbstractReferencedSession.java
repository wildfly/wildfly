/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Set;

import org.wildfly.clustering.function.BiConsumer;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.function.ToIntFunction;
import org.wildfly.clustering.function.ToLongFunction;
import org.wildfly.clustering.server.util.Reference;

import io.undertow.server.HttpServerExchange;

/**
 * An Undertow session decorator that delegates to a {@link UndertowSession} reference.
 * @author Paul Ferraro
 */
public abstract class AbstractReferencedSession extends AbstractSession {
    private static final Function<UndertowSession, String> IDENTIFIER = UndertowSession::getId;
    private static final Predicate<UndertowSession> VALID = UndertowSession::isValid;
    private static final ToLongFunction<UndertowSession> CREATION_TIME = UndertowSession::getCreationTime;
    private static final ToLongFunction<UndertowSession> LAST_ACCESSED_TIME = UndertowSession::getLastAccessedTime;
    private static final ToIntFunction<UndertowSession> MAX_INACTIVE_INTERVAL = UndertowSession::getMaxInactiveInterval;
    private static final BiConsumer<UndertowSession, Integer> SET_MAX_INACTIVE_INTERVAL = UndertowSession::setMaxInactiveInterval;
    private static final Function<UndertowSession, Set<String>> ATTRIBUTE_NAMES = UndertowSession::getAttributeNames;
    private static final BiFunction<UndertowSession, String, Object> GET_ATTRIBUTE = UndertowSession::getAttribute;
    private static final BiFunction<UndertowSession, String, Object> REMOVE_ATTRIBUTE = UndertowSession::removeAttribute;
    private static final BiConsumer<UndertowSession, HttpServerExchange> INVALIDATE = UndertowSession::invalidate;

    private final Reference<UndertowSession> reference;

    protected AbstractReferencedSession(UndertowSessionManager manager, Reference<UndertowSession> reference) {
        super(manager, reference.getReader().map(IDENTIFIER));
        this.reference = reference;
    }

    @Override
    public boolean isValid() {
        return this.reference.getReader().map(VALID.thenBox()).get();
    }

    @Override
    public long getCreationTime() {
        return this.reference.getReader().map(CREATION_TIME.thenBox()).get();
    }

    @Override
    public long getLastAccessedTime() {
        return this.reference.getReader().map(LAST_ACCESSED_TIME.thenBox()).get();
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.reference.getReader().map(MAX_INACTIVE_INTERVAL.thenBox()).get();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.reference.getReader().read(SET_MAX_INACTIVE_INTERVAL.composeUnary(Function.identity(), Function.of(interval)));
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.reference.getReader().map(ATTRIBUTE_NAMES).get();
    }

    @Override
    public Object getAttribute(String name) {
        return this.reference.getReader().map(GET_ATTRIBUTE.composeUnary(Function.identity(), Function.of(name))).get();
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return this.reference.getReader().map(new Function<>() {
            @Override
            public Object apply(UndertowSession session) {
                return session.setAttribute(name, value);
            }
        }).get();
    }

    @Override
    public Object removeAttribute(String name) {
        return this.reference.getReader().map(REMOVE_ATTRIBUTE.composeUnary(Function.identity(), Function.of(name))).get();
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        this.reference.getReader().read(INVALIDATE.composeUnary(Function.identity(), Function.of(exchange)));
    }
}
