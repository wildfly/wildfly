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

import java.util.LinkedList;
import java.util.List;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.jboss.as.clustering.infinispan.DefaultCacheContainer;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
@Listener
public class CacheContainerBuilder implements Builder<CacheContainer>, Service<CacheContainer> {

    private static final Logger log = Logger.getLogger(CacheContainerBuilder.class.getPackage().getName());

    private final InjectedValue<GlobalConfiguration> configuration = new InjectedValue<>();
    private final List<String> aliases = new LinkedList<>();
    private final String name;
    private final String defaultCache;

    private volatile CacheContainer container;

    public CacheContainerBuilder(String name, String defaultCache) {
        this.name = name;
        this.defaultCache = defaultCache;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheContainerServiceName.CACHE_CONTAINER.getServiceName(this.name);
    }

    @Override
    public ServiceBuilder<CacheContainer> build(ServiceTarget target) {
        ServiceBuilder<CacheContainer> builder = target.addService(this.getServiceName(), this)
                .addDependency(CacheContainerServiceName.CONFIGURATION.getServiceName(this.name), GlobalConfiguration.class, this.configuration)
        ;
        for (String alias : this.aliases) {
            builder.addAliases(CacheContainerServiceName.CACHE_CONTAINER.getServiceName(alias));
        }
        return builder.setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    public CacheContainerBuilder addAlias(String alias) {
        this.aliases.add(alias);
        return this;
    }

    @Override
    public CacheContainer getValue() {
        return this.container;
    }

    @Override
    public void start(StartContext context) {
        GlobalConfiguration config = this.configuration.getValue();
        this.container = new DefaultCacheContainer(config, this.defaultCache);
        this.container.addListener(this);
        this.container.start();
        log.debugf("%s cache container started", this.name);
    }

    @Override
    public void stop(StopContext context) {
        if ((this.container != null) && this.container.getStatus().allowInvocations()) {
            this.container.stop();
            this.container.removeListener(this);
            log.debugf("%s cache container stopped", this.name);
        }
    }

    @CacheStarted
    public void cacheStarted(CacheStartedEvent event) {
        InfinispanLogger.ROOT_LOGGER.cacheStarted(event.getCacheName(), this.name);
    }

    @CacheStopped
    public void cacheStopped(CacheStoppedEvent event) {
        InfinispanLogger.ROOT_LOGGER.cacheStopped(event.getCacheName(), this.name);
    }
}
