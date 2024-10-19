/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.Function;

import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;

/**
 * A function returning the client statistics for a remote cache.
 * @author Paul Ferraro
 */
public class RemoteCacheClientStatisticsFactory implements Function<RemoteCacheContainer, RemoteCacheClientStatisticsMXBean> {

    private final String cacheName;

    public RemoteCacheClientStatisticsFactory(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public RemoteCacheClientStatisticsMXBean apply(RemoteCacheContainer container) {
        return container.getCacheNames().contains(this.cacheName) ? container.getCache(this.cacheName).clientStatistics() : null;
    }
}
