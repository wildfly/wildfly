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
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractCacheConfigurationService implements Service<Configuration> {

    private final String name;
    private volatile Configuration config;

    private static final Logger log = Logger.getLogger(AbstractCacheConfigurationService.class.getPackage().getName());

    protected AbstractCacheConfigurationService(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public Configuration getValue() {
        return this.config;
    }

    protected abstract ConfigurationBuilder getConfigurationBuilder();

    protected abstract EmbeddedCacheManager getCacheContainer();

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {
        this.config = this.getConfigurationBuilder().build();

        EmbeddedCacheManager container = this.getCacheContainer();
        CacheMode mode = this.config.clustering().cacheMode();
        if (mode.isClustered() && (container.getTransport() == null)) {
            throw InfinispanLogger.ROOT_LOGGER.transportRequired(mode, this.name, container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName());
        }

        container.defineConfiguration(this.name, this.config);

        log.debugf("%s cache configuration started", this.name);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.config = null;
        log.debugf("%s cache configuration stopped", this.name);
    }
}
