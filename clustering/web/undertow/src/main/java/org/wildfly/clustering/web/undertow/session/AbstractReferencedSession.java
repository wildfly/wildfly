/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.server.util.Reference;

import io.undertow.server.HttpServerExchange;

/**
 * An Undertow session decorator that delegates to a {@link UndertowSession} reference.
 * @author Paul Ferraro
 */
public abstract class AbstractReferencedSession extends AbstractSession {

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
        return (value != null) ? this.reference.getReader().map(SET_ATTRIBUTE.composeUnary(Function.identity(), Function.of(Map.entry(name, value)))).get() : this.removeAttribute(name);
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
