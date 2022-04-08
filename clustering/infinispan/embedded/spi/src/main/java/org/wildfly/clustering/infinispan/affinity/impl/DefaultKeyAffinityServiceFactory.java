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

package org.wildfly.clustering.infinispan.affinity.impl;

import java.util.function.Predicate;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;

/**
 * Factory for a {@link KeyAffinityService} whose implementation varies depending on cache mode.
 * @author Paul Ferraro
 */
public class DefaultKeyAffinityServiceFactory implements KeyAffinityServiceFactory {

    @Override
    public <K> KeyAffinityService<K> createService(Cache<? extends K, ?> cache, KeyGenerator<K> generator, Predicate<Address> filter) {
        return cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new DefaultKeyAffinityService<>(cache, generator, filter) : new SimpleKeyAffinityService<>(generator);
    }
}
