/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Restartable;
import org.wildfly.clustering.ejb.remote.AffinitySupport;

/**
 * A SPI for managing beans.
 *
 * @author Paul Ferraro
 *
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <B> the batch type
 */
public interface BeanManager<K, V extends BeanInstance<K>, B extends Batch> extends Restartable, AffinitySupport<K>, BeanStatistics {
    Bean<K, V> createBean(V instance, K groupId);
    Bean<K, V> findBean(K id) throws TimeoutException;

    Supplier<K> getIdentifierFactory();

    Batcher<B> getBatcher();

    boolean isRemotable(Throwable throwable);
}
