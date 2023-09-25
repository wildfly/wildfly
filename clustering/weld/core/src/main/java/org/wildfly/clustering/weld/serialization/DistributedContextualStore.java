/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.serialization;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.jboss.weld.serialization.ContextualStoreImpl;
import org.jboss.weld.serialization.spi.helpers.SerializableContextual;
import org.jboss.weld.util.reflection.Reflections;
import org.wildfly.clustering.weld.contexts.PassivationCapableSerializableBean;
import org.wildfly.clustering.weld.contexts.PassivationCapableSerializableContextual;

/**
 * {@link org.jboss.weld.serialization.spi.ContextualStore} implementation for distributed applications.
 * @author Paul Ferraro
 */
public class DistributedContextualStore extends ContextualStoreImpl {

    private final String contextId;

    public DistributedContextualStore(String contextId) {
        super(contextId, null);
        this.contextId = contextId;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <C extends Contextual<I>, I> SerializableContextual<C, I> getSerializableContextual(Contextual<I> contextual) {
        if (contextual instanceof SerializableContextual<?, ?>) {
            return Reflections.cast(contextual);
        }
        if (contextual instanceof PassivationCapable) {
            return (contextual instanceof Bean) ? new PassivationCapableSerializableBean(this.contextId, Reflections.<Bean<I>>cast(contextual)) : new PassivationCapableSerializableContextual(this.contextId, contextual);
        }
        return super.getSerializableContextual(contextual);
    }
}
