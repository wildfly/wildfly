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

import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.logging.Logger;

import javax.ejb.NoSuchEJBException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Stuart Douglas
 */
public class TransactionLocalEntityCache implements ReadyEntityCache {


    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final ThreadLocal<Map<Object, EntityBeanComponentInstance>> cache = new ThreadLocal<Map<Object, EntityBeanComponentInstance>>();
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
        prepareCache();
        final Map<Object, EntityBeanComponentInstance> map = cache.get();
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
            if(cache.get() != null) {
                cache.get().remove(instance.getPrimaryKey());
            }
        }
    }

    @Override
    public void create(final EntityBeanComponentInstance instance) throws NoSuchEJBException {
        if (isTransactionActive()) {
            prepareCache();
            cache.get().put(instance.getPrimaryKey(), instance);
        }
    }

    @Override
    public void release(final EntityBeanComponentInstance instance, boolean success) {
        //TODO: this should probably be somewhere else
        //roll back unsuccessful removal
        if(!success && instance.isRemoved()) {
            instance.setRemoved(false);
        }
        instance.passivate();
        component.getPool().release(instance);
        if(cache.get() != null ){
            cache.get().remove(instance.getPrimaryKey());
        }

    }

    @Override
    public synchronized void start() {

    }

    @Override
    public synchronized void stop() {
    }

    private void prepareCache() {
        if (cache.get() == null) {
            cache.set(new HashMap<Object, EntityBeanComponentInstance>());
            if (transactionSynchronizationRegistry.getTransactionKey() != null) {
                transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
                    @Override
                    public void beforeCompletion() {

                    }

                    @Override
                    public void afterCompletion(final int status) {
                        cache.remove();
                    }
                });
            }
        }
    }

    private EntityBeanComponentInstance createInstance(Object pk) {
        final  EntityBeanComponentInstance instance = component.getPool().get();
        instance.associate(pk);
        return instance;
    }

    private boolean isTransactionActive() {
        return transactionSynchronizationRegistry.getTransactionKey() != null;
    }
}
