/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

import java.util.function.Supplier;

import org.jboss.as.clustering.service.DecoratedService;
import org.jboss.ejb.client.Affinity;
import org.wildfly.clustering.server.manager.Service;

/**
 * A stateful session bean cache decorator.
 * @author Paul Ferraro
 */
public class DecoratedStatefulSessionBeanCache<K, V extends StatefulSessionBeanInstance<K>> extends DecoratedService implements StatefulSessionBeanCache<K, V> {

    private final StatefulSessionBeanCache<K, V> cache;

    public DecoratedStatefulSessionBeanCache(StatefulSessionBeanCache<K, V> cache) {
        this(cache, cache);
    }

    protected DecoratedStatefulSessionBeanCache(StatefulSessionBeanCache<K, V> cache, Service service) {
        super(service);
        this.cache = cache;
    }

    @Override
    public int getActiveCount() {
        return this.cache.getActiveCount();
    }

    @Override
    public int getPassiveCount() {
        return this.cache.getPassiveCount();
    }

    @Override
    public Affinity getStrongAffinity() {
        return this.cache.getStrongAffinity();
    }

    @Override
    public Affinity getWeakAffinity(K id) {
        return this.cache.getWeakAffinity(id);
    }

    @Override
    public StatefulSessionBean<K, V> createStatefulSessionBean() {
        return this.cache.createStatefulSessionBean();
    }

    @Override
    public StatefulSessionBean<K, V> findStatefulSessionBean(K id) {
        return this.cache.findStatefulSessionBean(id);
    }

    @Override
    public Supplier<K> getIdentifierFactory() {
        return this.cache.getIdentifierFactory();
    }

    @Override
    public boolean isRemotable(Throwable throwable) {
        return this.cache.isRemotable(throwable);
    }
}
