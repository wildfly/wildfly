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

package org.jboss.as.test.clustering.cluster.infinispan.bean.remote;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.infinispan.client.hotrod.DefaultTemplate;
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
        this.container.getConfiguration().addRemoteCache(CACHE_NAME, builder -> builder.templateName(DefaultTemplate.LOCAL).forceReturnValues(true));
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
