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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.concurrent.atomic.AtomicBoolean;

import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.infinispan.BeanEntry;
import org.wildfly.clustering.ejb.infinispan.BeanGroup;
import org.wildfly.clustering.ejb.infinispan.BeanRemover;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;

/**
 * A {@link Bean} implementation backed by an infinispan cache.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class InfinispanBean<I, T> implements Bean<I, T> {

    private final I id;
    private final BeanEntry<I> entry;
    private final BeanGroup<I, T> group;
    private final Mutator mutator;
    private final BeanRemover<I, T> remover;
    private final Duration timeout;
    private final PassivationListener<T> listener;
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public InfinispanBean(I id, BeanEntry<I> entry, BeanGroup<I, T> group, Mutator mutator, BeanRemover<I, T> remover, Duration timeout, PassivationListener<T> listener) {
        this.id = id;
        this.entry = entry;
        this.group = group;
        this.mutator = mutator;
        this.remover = remover;
        this.timeout = timeout;
        this.listener = listener;
    }

    @Override
    public I getId() {
        return this.id;
    }

    @Override
    public I getGroupId() {
        return this.entry.getGroupId();
    }

    @Override
    public boolean isExpired() {
        return this.entry.isExpired(this.timeout) && this.group.isCloseable();
    }

    @Override
    public boolean isValid() {
        return this.valid.get();
    }

    @Override
    public void remove(RemoveListener<T> listener) {
        if (this.valid.compareAndSet(true, false)) {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Removing bean %s", this.id);
            this.remover.remove(this.id, listener);
        }
    }

    @Override
    public T acquire() {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Acquiring reference to bean %s", this.id);
        return this.group.getBean(this.id, this.listener);
    }

    @Override
    public boolean release() {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Releasing reference to bean %s", this.id);
        return this.group.releaseBean(this.id, this.listener);
    }

    @Override
    public void close() {
        if (this.valid.get()) {
            Instant lastAccessedTime = this.entry.getLastAccessedTime();
            Instant now = Instant.now();
            // Reduce precision to millis
            this.entry.setLastAccessedTime((now.getNano() % 1_000_000) > 0 ? now.with(ChronoField.MILLI_OF_SECOND, now.get(ChronoField.MILLI_OF_SECOND)) : now);
            if (lastAccessedTime != null) {
                this.mutator.mutate();
            }
        }
        if (this.group.isCloseable()) {
            this.group.close();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Bean)) return false;
        @SuppressWarnings("unchecked")
        Bean<I, T> bean = (Bean<I, T>) object;
        return this.id.equals(bean.getId());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
