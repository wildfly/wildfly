/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateful.cache.simple;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCache;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstanceFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.scheduler.LinkedScheduledEntries;
import org.wildfly.clustering.ee.cache.scheduler.LocalScheduler;

/**
 * A simple stateful session bean cache implementation.
 * Bean instances are stored in memory and are lost on undeploy, shutdown, or server crash.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class SimpleStatefulSessionBeanCache<K, V extends StatefulSessionBeanInstance<K>> implements StatefulSessionBeanCache<K, V>, Predicate<K>, Consumer<StatefulSessionBean<K, V>> {

    private final Map<K, V> instances = new ConcurrentHashMap<>();
    private final Consumer<K> remover = this.instances::remove;
    private final StatefulSessionBeanInstanceFactory<V> factory;
    private final Supplier<K> identifierFactory;
    private final Duration timeout;
    private final Affinity strongAffinity;

    private volatile Scheduler<K, Instant> scheduler;

    public SimpleStatefulSessionBeanCache(SimpleStatefulSessionBeanCacheConfiguration<K, V> configuration) {
        this.factory = configuration.getInstanceFactory();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.timeout = configuration.getTimeout();
        this.strongAffinity = new NodeAffinity(configuration.getEnvironment().getNodeName());
    }

    @Override
    public void start() {
        this.scheduler = (this.timeout != null) && !this.timeout.isZero() ? new LocalScheduler<>(new LinkedScheduledEntries<>(), this, Duration.ZERO) : null;
    }

    @Override
    public void stop() {
        if (this.scheduler != null) {
            this.scheduler.close();
        }
        for (V instance : this.instances.values()) {
            instance.removed();
        }
        this.instances.clear();
    }

    @Override
    public Affinity getStrongAffinity() {
        return this.strongAffinity;
    }

    @Override
    public Affinity getWeakAffinity(K id) {
        return Affinity.NONE;
    }

    @Override
    public void accept(StatefulSessionBean<K, V> bean) {
        if (this.timeout != null) {
            K id = bean.getId();
            if (this.scheduler != null) {
                // Timeout > 0, schedule bean to expire
                this.scheduler.schedule(id, Instant.now().plus(this.timeout));
            } else {
                // Timeout = 0, remove bean immediately
                this.test(id);
            }
        }
    }

    @Override
    public boolean test(K id) {
        V instance = this.instances.remove(id);
        if (instance != null) {
            instance.removed();
        }
        return true;
    }

    @Override
    public K createStatefulSessionBean() {
        if (CURRENT_GROUP.get() != null) {
            // An SFSB that uses a distributable cache cannot contain an SFSB that uses a simple cache
            throw EjbLogger.ROOT_LOGGER.incompatibleCaches();
        }
        V instance = this.factory.createInstance();
        K id = instance.getId();
        this.instances.put(id, instance);
        return id;
    }

    @Override
    public StatefulSessionBean<K, V> findStatefulSessionBean(K id) {
        V instance = this.instances.get(id);
        if (instance == null) return null;
        if (this.scheduler != null) {
            this.scheduler.cancel(id);
        }
        return new SimpleStatefulSessionBean<>(instance, this.remover, this);
    }

    @Override
    public int getActiveCount() {
        return this.instances.size();
    }

    @Override
    public int getPassiveCount() {
        return 0;
    }

    @Override
    public Supplier<K> getIdentifierFactory() {
        return this.identifierFactory;
    }
}
