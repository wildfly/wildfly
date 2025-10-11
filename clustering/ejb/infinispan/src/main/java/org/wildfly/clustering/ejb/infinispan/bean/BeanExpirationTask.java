/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;

/**
 * The bean expiration task triggered by the expiration scheduler.
 * @author Paul Ferraro
 */
public class BeanExpirationTask<K, V extends BeanInstance<K>, M> implements Predicate<K> {
    private final BeanFactory<K, V, M> beanFactory;
    private final Supplier<Batch> batchFactory;
    private final Consumer<V> expirationListener;

    BeanExpirationTask(BeanFactory<K, V, M> beanFactory, Supplier<Batch> batchFactory, Consumer<V> expirationListener) {
        this.beanFactory = beanFactory;
        this.batchFactory = batchFactory;
        this.expirationListener = expirationListener;
    }

    @Override
    public boolean test(K id) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Expiring stateful session bean %s", id);
        try (Batch batch = this.batchFactory.get()) {
            try {
                M value = this.beanFactory.tryValue(id);
                if (value != null) {
                    try (Bean<K, V> bean = this.beanFactory.createBean(id, value)) {
                        // Ensure bean is actually expired
                        if (bean.getMetaData().isExpired()) {
                            bean.remove(this.expirationListener);
                        }
                    }
                }
                return true;
            } catch (RuntimeException e) {
                batch.discard();
                throw e;
            }
        } catch (RuntimeException e) {
            InfinispanEjbLogger.ROOT_LOGGER.failedToExpireBean(e, id);
            return false;
        }
    }
}
