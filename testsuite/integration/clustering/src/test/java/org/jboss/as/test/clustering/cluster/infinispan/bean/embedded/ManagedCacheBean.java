/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.bean.embedded;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

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
public class ManagedCacheBean extends CacheBean {

    @Resource(lookup = "java:jboss/infinispan/cache/server/default")
    private org.infinispan.Cache<Key, Value> cache;

    @Override
    public BasicCache<Key, Value> get() {
        return this.cache;
    }
}
