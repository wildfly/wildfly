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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.clustering.marshalling.MarshalledValue;
import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.wildfly.clustering.ejb.infinispan.BeanGroupEntry;

/**
 * The cache entry of a bean group.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class InfinispanBeanGroupEntry<I, T> implements BeanGroupEntry<I, T> {

    private final MarshalledValue<Map<I, T>, MarshallingContext> beans;
    private final ConcurrentMap<I, AtomicInteger> usage = new ConcurrentHashMap<>();

    public InfinispanBeanGroupEntry(MarshalledValue<Map<I, T>, MarshallingContext> beans) {
        this.beans = beans;
    }

    @Override
    public MarshalledValue<Map<I, T>, MarshallingContext> getBeans() {
        return this.beans;
    }

    @Override
    public int incrementUsage(I id) {
        AtomicInteger count = this.usage.get(id);
        if (count == null) {
            count = new AtomicInteger();
            AtomicInteger old = this.usage.putIfAbsent(id, count);
            if (old != null) {
                count = old;
            }
        }
        return count.getAndIncrement();
    }

    @Override
    public int decrementUsage(I id) {
        AtomicInteger count = this.usage.get(id);
        return (count != null) ? count.decrementAndGet() : 0;
    }

    @Override
    public int totalUsage() {
        int total = 0;
        for (AtomicInteger count: this.usage.values()) {
            total += count.get();
        }
        return total;
    }
}
