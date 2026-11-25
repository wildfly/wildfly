/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import java.io.ObjectStreamException;
import java.io.Serializable;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.jboss.weld.Container;
import org.jboss.weld.bean.ForwardingBean;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.util.Beans;

/**
 * @author Paul Ferraro
 */
public class PassivationCapableSerializableBean<B extends Bean<I> & PassivationCapable, I> extends ForwardingBean<I> implements PassivationCapableContextual<B, I>, MarshallableContextual<B> {
    private static final long serialVersionUID = -840179425829721439L;

    private final String contextId;
    private final BeanIdentifier identifier;
    private final B instance;

    public PassivationCapableSerializableBean(String contextId, B instance) {
        this(contextId, Beans.getIdentifier(instance, Container.instance(contextId).services().get(ContextualStore.class)), instance);
    }

    PassivationCapableSerializableBean(String contextId, BeanIdentifier identifier) {
        this(contextId, identifier, Container.instance(contextId).services().get(ContextualStore.class).getContextual(identifier));
    }

    private PassivationCapableSerializableBean(String contextId, BeanIdentifier identifier, B instance) {
        this.contextId = contextId;
        this.instance = instance;
        this.identifier = identifier;
    }

    @Override
    public BeanIdentifier getIdentifier() {
        return this.identifier;
    }

    @Override
    public B getInstance() {
        return this.instance;
    }

    @Override
    public String getId() {
        return this.instance.getId();
    }

    @Override
    public B get() {
        return this.instance;
    }

    @Override
    public Bean<I> delegate() {
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
