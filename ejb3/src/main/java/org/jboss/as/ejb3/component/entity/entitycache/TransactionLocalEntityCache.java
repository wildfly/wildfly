/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.component.entity.entitycache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ejb.NoSuchEJBException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.logging.Logger;

/**
 * Cache of entity bean component instances by transaction key
 * @author Stuart Douglas
 */
public class TransactionLocalEntityCache implements ReadyEntityCache {


    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final ConcurrentMap<Object, Map<Object, EntityBeanComponentInstance>> cache = new ConcurrentHashMap<Object, Map<Object, EntityBeanComponentInstance>>(Runtime.getRuntime().availableProcessors());
    private final EntityBeanComponent component;

    private static final Logger logger = Logger.getLogger(TransactionLocalEntityCache.class);

    public TransactionLocalEntityCache(final EntityBeanComponent component) {
        this.component = component;
        this.transactionSynchronizationRegistry = component.getTransactionSynchronizationRegistry();
    }

    @Override
    public EntityBeanComponentInstance get(final Object key) throws NoSuchEJBException {
        if (!isTransactionActive()) {
            return createInstance(key);
        }
        final Map<Object, EntityBeanComponentInstance> map = prepareCache();
        EntityBeanComponentInstance instance = map.get(key);
        if (instance == null) {
            instance = createInstance(key);
            map.put(key, instance);
        }
        return instance;
    }

    @Override
    public void discard(final EntityBeanComponentInstance instance) {
        if (isTransactionActive()) {
            final Object key = transactionSynchronizationRegistry.getTransactionKey();
            final Map<Object, EntityBeanComponentInstance> map = cache.get(key);
            if (map != null) {
                map.remove(instance.getPrimaryKey());
            }
        }
    }

    @Override
    public void create(final EntityBeanComponentInstance instance) throws NoSuchEJBException {
        if (isTransactionActive()) {
            final Map<Object, EntityBeanComponentInstance> map = prepareCache();
            map.put(instance.getPrimaryKey(), instance);
        }
    }

    @Override
    public void release(final EntityBeanComponentInstance instance, boolean success) {
        //TODO: this should probably be somewhere else
        //roll back unsuccessful removal
        if (!success && instance.isRemoved()) {
            instance.setRemoved(false);
        }
        instance.passivate();
        component.getPool().release(instance);
        discard(instance);
    }

    public void reference(EntityBeanComponentInstance instance) {
    }

    @Override
    public synchronized void start() {

    }

    @Override
    public synchronized void stop() {
    }

    private Map<Object, EntityBeanComponentInstance> prepareCache() {
        final Object key = transactionSynchronizationRegistry.getTransactionKey();
        Map<Object, EntityBeanComponentInstance> map = cache.get(key);
        if (map != null) {
            return map;
        }
        map = Collections.synchronizedMap(new HashMap<Object, EntityBeanComponentInstance>());
        final Map<Object, EntityBeanComponentInstance> existing = cache.putIfAbsent(key, map);
        if (existing != null) {
            map = existing;
        }
        transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {

            }

            @Override
            public void afterCompletion(final int status) {
                cache.remove(key);
            }
        });
        return map;
    }

    private EntityBeanComponentInstance createInstance(Object pk) {
        final EntityBeanComponentInstance instance = component.getPool().get();
        instance.associate(pk);
        return instance;
    }

    private boolean isTransactionActive() {
        return transactionSynchronizationRegistry.getTransactionKey() != null;
    }
}
