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

import org.wildfly.clustering.ejb.infinispan.BeanGroupEntry;
import org.wildfly.clustering.marshalling.jboss.MarshalledValue;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;

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
        return this.usage.computeIfAbsent(id, (I key) -> new AtomicInteger(0)).getAndIncrement();
    }

    @Override
    public int decrementUsage(I id) {
        AtomicInteger count = this.usage.get(id);
        return (count != null) ? count.decrementAndGet() : 0;
    }

    @Override
    public int totalUsage() {
        return this.usage.values().stream().mapToInt((AtomicInteger usage) -> usage.get()).sum();
    }
}
