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

import org.infinispan.client.hotrod.Flag;
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
    @Resource(lookup = "java:jboss/infinispan/remote-container/remote")
    private RemoteCacheContainer container;

    private RemoteCache<Key, Value> cache;

    @PostConstruct
    public void init() {
        this.cache = this.container.getCache("query");
        this.cache.start();
    }

    @PreDestroy
    public void destroy() {
        this.cache.stop();
    }

    @Override
    public BasicCache<Key, Value> get() {
        return this.cache.withFlags(Flag.FORCE_RETURN_VALUE);
    }
}
