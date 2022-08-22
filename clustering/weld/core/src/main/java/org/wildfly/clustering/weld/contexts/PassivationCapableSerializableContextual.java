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
    private C instance;

    public PassivationCapableSerializableContextual(String contextId, C instance) {
        this(contextId, Beans.getIdentifier(instance, Container.instance(contextId).services().get(ContextualStore.class)), instance);
    }

    PassivationCapableSerializableContextual(String contextId, BeanIdentifier identifier) {
        this(contextId, identifier, null);
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
        // Resolve contextual lazily
        if (this.instance == null) {
            Container container = Container.instance(this.contextId);
            ContextualStore store = container.services().get(ContextualStore.class);
            this.instance = store.getContextual(this.identifier);
        }
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
