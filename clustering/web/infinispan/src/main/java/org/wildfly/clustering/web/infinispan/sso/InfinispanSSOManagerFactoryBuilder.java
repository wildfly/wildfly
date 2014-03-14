/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.sso;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.subsystem.CacheConfigurationService;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.infinispan.subsystem.GlobalComponentRegistryService;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilder;

public class InfinispanSSOManagerFactoryBuilder implements SSOManagerFactoryBuilder {

    public static final String DEFAULT_CACHE_CONTAINER = "web";

    @Override
    public <A, D> ServiceBuilder<SSOManagerFactory<A, D>> build(ServiceTarget target, ServiceName name, String host) {
        String containerName = DEFAULT_CACHE_CONTAINER;
        ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName);
        String templateCacheName = CacheContainer.DEFAULT_CACHE_ALIAS;
        String cacheName = host;
        ServiceName cacheConfigurationServiceName = CacheConfigurationService.getServiceName(containerName, cacheName);
        ServiceName cacheServiceName = CacheService.getServiceName(containerName, cacheName);

        SSOCacheConfigurationService.build(target, containerName, cacheName, templateCacheName)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        final InjectedValue<EmbeddedCacheManager> cacheContainer = new InjectedValue<>();
        CacheService.Dependencies dependencies = new CacheService.Dependencies() {
            @Override
            public EmbeddedCacheManager getCacheContainer() {
                return cacheContainer.getValue();
            }

            @Override
            public XAResourceRecoveryRegistry getRecoveryRegistry() {
                return null;
            }
        };
        AsynchronousService.addService(target, cacheServiceName, new CacheService<>(cacheName, dependencies))
                .addDependency(GlobalComponentRegistryService.getServiceName(containerName))
                .addDependency(cacheConfigurationServiceName)
                .addDependency(containerServiceName, EmbeddedCacheManager.class, cacheContainer)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;

        return InfinispanSSOManagerFactory.build(target, name, containerName, cacheName);
    }
}
