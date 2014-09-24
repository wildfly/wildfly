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
package org.wildfly.clustering.web.infinispan.session;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.infinispan.subsystem.AbstractCacheConfigurationService;
import org.jboss.as.clustering.infinispan.subsystem.CacheConfigurationService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * Web session cache configuration service.
 * @author Paul Ferraro
 */
public class SessionCacheConfigurationService extends AbstractCacheConfigurationService {

    public static ServiceBuilder<Configuration> build(ServiceTarget target, String containerName, String cacheName, String templateCacheName) {
        SessionCacheConfigurationService service = new SessionCacheConfigurationService(cacheName);
        return target.addService(CacheConfigurationService.getServiceName(containerName, cacheName), service)
                .addDependency(EmbeddedCacheManagerService.getServiceName(containerName), EmbeddedCacheManager.class, service.container)
                .addDependency(CacheConfigurationService.getServiceName(containerName, templateCacheName), Configuration.class, service.configuration)
        ;
    }

    private final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
    private final InjectedValue<Configuration> configuration = new InjectedValue<>();

    private SessionCacheConfigurationService(String name) {
        super(name);
    }

    @Override
    protected ConfigurationBuilder getConfigurationBuilder() {
        Configuration config = this.configuration.getValue();
        ConfigurationBuilder builder = new ConfigurationBuilder().read(config);
        builder.invocationBatching().enable();
        builder.storeAsBinary().disable().storeKeysAsBinary(false).storeValuesAsBinary(false);
        builder.locking().isolationLevel(IsolationLevel.REPEATABLE_READ);
        return builder;
    }

    @Override
    protected EmbeddedCacheManager getCacheContainer() {
        return this.container.getValue();
    }
}
