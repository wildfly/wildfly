/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.bean.embedded;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
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
public class CreatedCacheBean extends CacheBean {

    @Resource(lookup = "java:jboss/infinispan/container/server")
    private EmbeddedCacheManager container;
    @Resource(lookup = "java:jboss/infinispan/configuration/server/default")
    private Configuration config;

    private org.infinispan.Cache<Key, Value> cache;

    @PostConstruct
    public void init() {
        this.cache = this.container.administration().createCache(this.getClass().getSimpleName(), this.config);
        this.cache.start();
    }

    @PreDestroy
    public void destroy() {
        this.cache.stop();
        this.container.administration().removeCache(this.getClass().getSimpleName());
    }

    @Override
    public BasicCache<Key, Value> get() {
        return this.cache;
    }
}
