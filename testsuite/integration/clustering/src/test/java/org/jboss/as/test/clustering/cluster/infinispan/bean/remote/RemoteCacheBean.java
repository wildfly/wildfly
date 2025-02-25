/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.bean.remote;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.commons.api.BasicCache;
import org.jboss.as.test.clustering.cluster.infinispan.bean.Cache;
import org.jboss.as.test.clustering.cluster.infinispan.bean.CacheBean;
import org.jboss.as.test.clustering.cluster.infinispan.bean.Key;
import org.jboss.as.test.clustering.cluster.infinispan.bean.Value;

/**
 * @author Paul Ferraro
 */
@Local(Cache.class)
@Singleton
@Startup
public class RemoteCacheBean extends CacheBean {
    private static final String CACHE_NAME = RemoteCacheBean.class.getName();

    @Resource(lookup = "java:jboss/infinispan/remote-container/remote")
    private RemoteCacheContainer container;

    private RemoteCache<Key, Value> cache;

    @PostConstruct
    public void init() {
        this.container.getConfiguration().addRemoteCache(CACHE_NAME, builder -> builder.configuration("{\"local-cache\": {}}").forceReturnValues(true));
        this.cache = this.container.getCache(CACHE_NAME);
        this.cache.start();
    }

    @PreDestroy
    public void destroy() {
        this.cache.stop();
        this.container.getConfiguration().removeRemoteCache(CACHE_NAME);
    }

    @Override
    public BasicCache<Key, Value> get() {
        return this.cache;
    }
}
