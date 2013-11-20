/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan.group;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.jboss.as.clustering.infinispan.invoker.Remover;
import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.infinispan.BeanGroup;
import org.wildfly.clustering.ejb.infinispan.BeanGroupEntry;
import org.wildfly.clustering.ejb.infinispan.InfinispanEjbMessages;

/**
 * A {@link org.wildfly.clustering.ejb.infinispan.BeanGroup} implementation backed by an infinispan cache.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class InfinispanBeanGroup<G, I, T> implements BeanGroup<G, I, T> {

    private final G id;
    private final BeanGroupEntry<I, T> entry;
    private final MarshallingContext context;
    private final Mutator mutator;
    private final Remover<G> remover;

    public InfinispanBeanGroup(G id, BeanGroupEntry<I, T> entry, MarshallingContext context, Mutator mutator, Remover<G> remover) {
        this.id = id;
        this.entry = entry;
        this.context = context;
        this.mutator = mutator;
        this.remover = remover;
    }

    @Override
    public G getId() {
        return this.id;
    }

    private Map<I, T> beans() {
        try {
            return this.entry.getBeans().get(this.context);
        } catch (IOException | ClassNotFoundException e) {
            throw InfinispanEjbMessages.MESSAGES.deserializationFailure(e, this.id);
        }
    }

    @Override
    public Set<I> getBeans() {
        return this.beans().keySet();
    }

    @Override
    public T getBean(I id, PassivationListener<T> listener) {
        T bean = this.beans().get(id);
        if (bean != null) {
            int usage = this.entry.incrementUsage(id);
            if ((usage == 0) && (listener != null)) {
                listener.postActivate(bean);
            }
        }
        return bean;
    }

    @Override
    public T removeBean(I id) {
        return this.beans().remove(id);
    }

    @Override
    public void addBean(I id, T bean) {
        this.beans().put(id, bean);
        this.entry.incrementUsage(id);
    }

    @Override
    public boolean releaseBean(I id, PassivationListener<T> listener) {
        int usage = this.entry.decrementUsage(id);
        boolean released = usage == 0;
        if (released) {
            this.prePassivate(id, listener);
        }
        return released;
    }

    @Override
    public boolean isCloseable() {
        return this.entry.totalUsage() == 0;
    }

    @Override
    public void close() {
        if (!this.beans().isEmpty()) {
            this.mutator.mutate();
        } else {
            this.remover.remove(this.id);
        }
    }

    @Override
    public void prePassivate(I id, PassivationListener<T> listener) {
        if (listener != null) {
            T bean = this.beans().get(id);
            if (bean != null) {
                listener.prePassivate(bean);
            }
        }
    }

    @Override
    public void postActivate(I id, PassivationListener<T> listener) {
        if (listener != null) {
            T bean = this.beans().get(id);
            if (bean != null) {
                listener.postActivate(bean);
            }
        }
    }
}
