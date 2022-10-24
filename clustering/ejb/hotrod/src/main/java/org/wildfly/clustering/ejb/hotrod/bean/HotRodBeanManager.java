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

package org.wildfly.clustering.ejb.hotrod.bean;

import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfiguration;
import org.infinispan.commons.CacheException;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.jboss.logging.Logger;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.hotrod.tx.HotRodBatcher;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanExpirationConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.ejb.cache.bean.MutableBean;
import org.wildfly.clustering.ejb.cache.bean.OnCloseBean;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.client.listener.ListenerRegistrar;
import org.wildfly.clustering.infinispan.client.listener.ListenerRegistration;

/**
 * A {@link BeanManager} implementation backed by a remote Infinispan cluster accessed via HotRod.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public class HotRodBeanManager<K, V extends BeanInstance<K>, M> implements BeanManager<K, V, TransactionBatch> {
    private static final Logger LOGGER = Logger.getLogger(BeanManagerNearCacheFactory.class);

    private final RemoteCache<?, ?> cache;
    private final BeanFactory<K, V, M> beanFactory;
    private final Supplier<K> identifierFactory;
    private final BeanExpirationConfiguration<K, V> expiration;
    private final Batcher<TransactionBatch> batcher;
    private final Affinity strongAffinity;
    private final Affinity weakAffinity;
    private final UnaryOperator<Bean<K, V>> transformer;
    private final ListenerRegistrar expirationListenerRegistrar;

    private volatile ListenerRegistration expirationListenerRegistration;

    public HotRodBeanManager(HotRodBeanManagerConfiguration<K, V, M> configuration, ListenerRegistrar expirationListenerRegistrar) {
        this.expirationListenerRegistrar = expirationListenerRegistrar;
        this.beanFactory = configuration.getBeanFactory();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.expiration = configuration.getExpiration();
        this.cache = configuration.getCache();
        this.batcher = new HotRodBatcher(this.cache);
        Group group = configuration.getGroup();
        this.strongAffinity = new ClusterAffinity(group.getName());
        RemoteCacheConfiguration cacheConfiguration = this.cache.getRemoteCacheContainer().getConfiguration().remoteCaches().get(this.cache.getName());
        this.weakAffinity = cacheConfiguration.nearCacheMode().enabled() ? new NodeAffinity(group.getLocalMember().getName()) : Affinity.NONE;
        // If bean has timeout = 0, remove bean on close
        Consumer<Bean<K, V>> closeTask = (this.expiration != null) && this.expiration.getTimeout().isZero() ? bean -> bean.remove(this.expiration.getExpirationListener()) : null;
        this.transformer = (closeTask != null) ? bean -> new OnCloseBean<>(bean, closeTask) : UnaryOperator.identity();
    }

    @Override
    public void start() {
        // We only need to listen for server-side expirations if bean timeout is non-zero.
        if ((this.expiration != null) && !this.expiration.getTimeout().isZero()) {
            this.expirationListenerRegistration = this.expirationListenerRegistrar.register();
        }
    }

    @Override
    public void stop() {
        if (this.expirationListenerRegistration != null) {
            this.expirationListenerRegistration.close();
        }
    }

    @Override
    public Affinity getStrongAffinity() {
        return this.strongAffinity;
    }

    @Override
    public Affinity getWeakAffinity(K id) {
        return this.weakAffinity;
    }

    @Override
    public int getActiveCount() {
        // Since we cannot iterate/filter the near cache directly, we estimate the number of beans based on the near cache size,
        // assuming that most beans will have approximately 3 cache entries (creation metadata entry + access metadata entry + group entry).
        return (int) (this.cache.clientStatistics().getNearCacheSize() / 3);
    }

    @Override
    public int getPassiveCount() {
        return this.cache.size() / 3;
    }

    @Override
    public Bean<K, V> createBean(V instance, K groupId) {
        K id = instance.getId();
        LOGGER.tracef("Creating bean %s associated with group %s", id, groupId);
        M value = this.beanFactory.createValue(instance, groupId);
        MutableBean<K, V> bean = this.beanFactory.createBean(id, value);
        bean.setInstance(instance);
        return bean;
    }

    @Override
    public Bean<K, V> findBean(K id) throws TimeoutException {
        LOGGER.tracef("Locating bean %s", id);
        M value = this.beanFactory.findValue(id);
        if (value == null) {
            LOGGER.debugf("Could not find bean %s", id);
            return null;
        }
        @SuppressWarnings("resource")
        Bean<K, V> bean = this.beanFactory.createBean(id, value);
        if (bean.getInstance() == null) {
            LOGGER.tracef("Bean %s metadata was found, but bean instance was not, most likely due to passivation failure.", id);
            try {
                this.beanFactory.purge(id);
            } finally {
                bean.close();
            }
            return null;
        }
        if (bean.getMetaData().isExpired()) {
            LOGGER.debugf("Bean %s found, but was expired", id);
            try {
                bean.remove(this.expiration.getExpirationListener());
            } finally {
                bean.close();
            }
            return null;
        }
        return this.transformer.apply(bean);
    }

    @Override
    public Supplier<K> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public boolean isRemotable(Throwable exception) {
        return !(exception instanceof CacheException) && (exception.getCause() == null || this.isRemotable(exception.getCause()));
    }
}
