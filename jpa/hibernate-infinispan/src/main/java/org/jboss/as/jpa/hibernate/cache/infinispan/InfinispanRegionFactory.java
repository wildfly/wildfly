/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate.cache.infinispan;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 */
public class InfinispanRegionFactory extends org.hibernate.cache.infinispan.InfinispanRegionFactory {
    private static final long serialVersionUID = -3277051412715973863L;

    public static final String CACHE_CONTAINER = "hibernate.cache.infinispan.container";
    public static final String DEFAULT_CACHE_CONTAINER = "hibernate";

    public InfinispanRegionFactory() {
        super();
    }

    public InfinispanRegionFactory(Properties props) {
        super(props);
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties) throws CacheException {
        String container = ConfigurationHelper.getString(CACHE_CONTAINER, properties, DEFAULT_CACHE_CONTAINER);
        ServiceName serviceName = EmbeddedCacheManagerService.getServiceName(container);
        return (EmbeddedCacheManager) CurrentServiceContainer.getServiceContainer().getRequiredService(serviceName).getValue();
    }

    @Override
    public void stop() {
        // Do not attempt to stop our cache manager because it wasn't created by this region factory.
    }
}
