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
package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.clustering.ejb.infinispan.BeanEntry;
import org.wildfly.clustering.ejb.infinispan.BeanGroup;
import org.wildfly.clustering.ejb.infinispan.BeanRemover;

/**
 * A {@link Bean} implementation backed by an infinispan cache.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class InfinispanBean<G, I, T> implements Bean<G, I, T> {

    private final I id;
    private final BeanEntry<G> entry;
    private final BeanGroup<G, I, T> group;
    private final BeanRemover<I, T> remover;
    private final Time timeout;
    private final PassivationListener<T> listener;

    public InfinispanBean(I id, BeanEntry<G> entry, BeanGroup<G, I, T> group, BeanRemover<I, T> remover, Time timeout, PassivationListener<T> listener) {
        this.id = id;
        this.entry = entry;
        this.group = group;
        this.remover = remover;
        this.timeout = timeout;
        this.listener = listener;
    }

    @Override
    public I getId() {
        return this.id;
    }

    @Override
    public G getGroupId() {
        return this.entry.getGroupId();
    }

    @Override
    public boolean isExpired() {
        if (this.timeout == null) return false;
        long timeout = this.timeout.convert(TimeUnit.MILLISECONDS);
        return (timeout > 0) ? ((System.currentTimeMillis() - this.entry.getLastAccessedTime().getTime()) > timeout) : false;
    }

    @Override
    public void remove(RemoveListener<T> listener) {
        this.remover.remove(this.id, listener);
        this.close();
    }

    @Override
    public T acquire() {
        return this.group.getBean(this.id, this.listener);
    }

    @Override
    public boolean release() {
        return this.group.releaseBean(this.id, this.listener);
    }

    @Override
    public void close() {
        this.entry.setLastAccessedTime(new Date());
        if (this.group.isCloseable()) {
            this.group.close();
        }
    }
}
