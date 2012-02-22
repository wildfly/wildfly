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

package org.jboss.as.jpa.hibernate3.infinispan;

import java.util.Properties;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Infinispan-backed region factory that uses retrieves its cache manager from the Infinispan subsystem.
 * @author Paul Ferraro
 */
public class SharedInfinispanRegionFactory extends InfinispanRegionFactory {

    public SharedInfinispanRegionFactory() {
        super();
    }

    public SharedInfinispanRegionFactory(Properties props) {
        super(props);
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties) {
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        ServiceName serviceName = EmbeddedCacheManagerService.getServiceName(container);
        ServiceRegistry registry = CurrentServiceContainer.getServiceContainer();
        return (EmbeddedCacheManager) registry.getRequiredService(serviceName).getValue();
    }

    @Override
    public void stop() {
        // Do not attempt to stop our cache manager because it wasn't created by this region factory.
    }
}
