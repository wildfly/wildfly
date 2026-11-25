/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

import java.util.concurrent.TimeoutException;

import org.wildfly.clustering.ejb.remote.AffinitySupport;
import org.wildfly.clustering.server.manager.Manager;

/**
 * A SPI for managing beans.
 *
 * @author Paul Ferraro
 *
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <B> the batch type
 */
public interface BeanManager<K, V extends BeanInstance<K>> extends Manager<K>, AffinitySupport<K>, BeanStatistics, AutoCloseable {
    Bean<K, V> createBean(V instance, K groupId);
    Bean<K, V> findBean(K id) throws TimeoutException;

    boolean isRemotable(Throwable throwable);

    @Override
    void close();
}
