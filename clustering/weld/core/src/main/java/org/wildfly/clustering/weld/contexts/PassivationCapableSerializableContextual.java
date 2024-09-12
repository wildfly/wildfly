/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import java.io.ObjectStreamException;
import java.io.Serializable;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.jboss.weld.Container;
import org.jboss.weld.bean.WrappedContextual;
import org.jboss.weld.contexts.ForwardingContextual;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.util.Beans;

/**
 * @author Paul Ferraro
 */
public class PassivationCapableSerializableContextual<C extends Contextual<I> & PassivationCapable, I> extends ForwardingContextual<I> implements PassivationCapableContextual<C, I>, WrappedContextual<I>, MarshallableContextual<C> {
    private static final long serialVersionUID = 5113888683790476497L;

    private final String contextId;
    private final BeanIdentifier identifier;
    private final C instance;

    public PassivationCapableSerializableContextual(String contextId, C instance) {
        this(contextId, Beans.getIdentifier(instance, Container.instance(contextId).services().get(ContextualStore.class)), instance);
    }

    PassivationCapableSerializableContextual(String contextId, BeanIdentifier identifier) {
        this(contextId, identifier, Container.instance(contextId).services().get(ContextualStore.class).getContextual(identifier));
    }

    private PassivationCapableSerializableContextual(String contextId, BeanIdentifier identifier, C instance) {
        this.contextId = contextId;
        this.identifier = identifier;
        this.instance = instance;
    }

    @Override
    public BeanIdentifier getIdentifier() {
        return this.identifier;
    }

    @Override
    public C getInstance() {
        return this.instance;
    }

    @Override
    public String getId() {
        return this.get().getId();
    }

    @Override
    public C get() {
        return this.instance;
    }

    @Override
    public Contextual<I> delegate() {
        return this.get();
    }

    @Override
    public String getContextId() {
        return this.contextId;
    }

    @SuppressWarnings("unused")
    private Object writeReplace() throws ObjectStreamException {
        return (this.instance instanceof Serializable) ? this : new PassivationCapableSerializableContextualProxy<>(this.contextId, this.identifier);
    }
}
