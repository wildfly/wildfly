/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
