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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.affinity.impl.KeyAffinityServiceImpl;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.security.manager.action.GetAccessControlContextAction;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossThreadFactory;

import static java.security.AccessController.doPrivileged;

/**
 * Key affinity service factory that will only generates keys for use by the local node.
 * Returns a trivial implementation if the specified cache is not distributed.
 * @author Paul Ferraro
 */
public class KeyAffinityServiceFactoryBuilder implements Builder<KeyAffinityServiceFactory>, Service<KeyAffinityServiceFactory>, KeyAffinityServiceFactory {

    private final String containerName;
    private volatile int bufferSize = 10;
    private volatile ExecutorService executor;

    public KeyAffinityServiceFactoryBuilder(String containerName) {
        this.containerName = containerName;
    }

    public KeyAffinityServiceFactoryBuilder bufferSize(int size) {
        this.bufferSize = size;
        return this;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheContainerServiceName.AFFINITY.getServiceName(this.containerName);
    }

    @Override
    public ServiceBuilder<KeyAffinityServiceFactory> build(ServiceTarget target) {
        return new AsynchronousServiceBuilder<>(this.getServiceName(), this).startSynchronously().build(target)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public KeyAffinityServiceFactory getValue() {
        return this;
    }

    @Override
    public void start(StartContext context) {
        final ThreadGroup threadGroup = new ThreadGroup("KeyAffinityService ThreadGroup");
        final String namePattern = "KeyAffinityService Thread Pool -- %t";
        final ThreadFactory threadFactory = new JBossThreadFactory(threadGroup, Boolean.FALSE, null, namePattern, null, null, doPrivileged(GetAccessControlContextAction.getInstance()));

        this.executor = Executors.newCachedThreadPool(threadFactory);
    }

    @Override
    public void stop(StopContext context) {
        this.executor.shutdown();
    }

    @Override
    public <K> KeyAffinityService<K> createService(Cache<K, ?> cache, KeyGenerator<K> generator) {
        boolean clustered = cache.getCacheConfiguration().clustering().cacheMode().isClustered();
        return clustered ? new LifecycleKeyAffinityService<>(this.executor, cache, generator, this.bufferSize, Collections.singleton(cache.getCacheManager().getAddress())) : new SimpleKeyAffinityService<>(generator);
    }

    // Workaround for ISPN-4969
    // This implementation needs to be explicitly started and stopped
    private static class LifecycleKeyAffinityService<K> extends KeyAffinityServiceImpl<K> {

        LifecycleKeyAffinityService(Executor executor, Cache<? extends K, ?> cache, KeyGenerator<? extends K> keyGenerator, int bufferSize, Collection<Address> filter) {
            super(executor, cache, keyGenerator, bufferSize, filter, false);
        }

        @Override
        public void handleCacheStopped(CacheStoppedEvent event) {
            // Do nothing, we will stop the service manually
        }
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
