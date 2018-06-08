/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Capability.KEY_AFFINITY_FACTORY;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.affinity.impl.KeyAffinityServiceImpl;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Key affinity service factory that will only generates keys for use by the local node.
 * Returns a trivial implementation if the specified cache is not distributed.
 * @author Paul Ferraro
 */
public class KeyAffinityServiceFactoryServiceConfigurator extends CapabilityServiceNameProvider implements ServiceConfigurator, Function<ExecutorService, KeyAffinityServiceFactory>, Supplier<ExecutorService> {

    private volatile int bufferSize = 100;

    public KeyAffinityServiceFactoryServiceConfigurator(PathAddress address) {
        super(KEY_AFFINITY_FACTORY, address);
    }

    public KeyAffinityServiceFactoryServiceConfigurator setBufferSize(int size) {
        this.bufferSize = size;
        return this;
    }

    @Override
    public ExecutorService get() {
        ThreadGroup threadGroup = new ThreadGroup("KeyAffinityService ThreadGroup");
        String namePattern = "KeyAffinityService Thread Pool -- %t";
        PrivilegedAction<ThreadFactory> action = () -> new JBossThreadFactory(threadGroup, Boolean.FALSE, null, namePattern, null, null);
        return Executors.newCachedThreadPool(doPrivileged(action));
    }

    @Override
    public KeyAffinityServiceFactory apply(ExecutorService executor) {
        int bufferSize = this.bufferSize;
        return new KeyAffinityServiceFactory() {
            @Override
            public <K> KeyAffinityService<K> createService(Cache<K, ?> cache, KeyGenerator<K> generator) {
                CacheMode mode = cache.getCacheConfiguration().clustering().cacheMode();
                return mode.isDistributed() || mode.isReplicated() ? new KeyAffinityServiceImpl<>(executor, cache, generator, bufferSize, Collections.singleton(cache.getCacheManager().getAddress()), false) : new SimpleKeyAffinityService<>(generator);
            }
        };
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).startSynchronously().build(target);
        Consumer<KeyAffinityServiceFactory> affinityFactory = builder.provides(this.getServiceName());
        Service service = new FunctionalService<>(affinityFactory, this, this, ExecutorService::shutdown);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    private static class SimpleKeyAffinityService<K> implements KeyAffinityService<K> {
        private final KeyGenerator<K> generator;
        private volatile boolean started = false;

        SimpleKeyAffinityService(KeyGenerator<K> generator) {
            this.generator = generator;
        }

        @Override
        public void start() {
            this.started = true;
        }

        @Override
        public void stop() {
            this.started = false;
        }

        @Override
        public K getKeyForAddress(Address address) {
            return this.generator.getKey();
        }

        @Override
        public K getCollocatedKey(K otherKey) {
            return this.generator.getKey();
        }

        @Override
        public boolean isStarted() {
            return this.started;
        }
    }
}
