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

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.ALIASES;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Capability.CONTAINER;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.BatcherFactory;
import org.jboss.as.clustering.infinispan.DefaultCacheContainer;
import org.jboss.as.clustering.infinispan.InfinispanBatcherFactory;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
@Listener
public class CacheContainerBuilder extends CapabilityServiceNameProvider implements ResourceServiceBuilder<CacheContainer>, Function<EmbeddedCacheManager, CacheContainer>, Supplier<EmbeddedCacheManager>, Consumer<EmbeddedCacheManager> {

    private final List<ServiceName> aliases = new LinkedList<>();
    private final String name;
    private final ValueDependency<GlobalConfiguration> configuration;
    private final BatcherFactory batcherFactory = new InfinispanBatcherFactory();

    public CacheContainerBuilder(PathAddress address) {
        super(CONTAINER, address);
        this.name = address.getLastElement().getValue();
        this.configuration = new InjectedValueDependency<>(CacheContainerResourceDefinition.Capability.CONFIGURATION.getServiceName(address), GlobalConfiguration.class);
    }

    @Override
    public CacheContainer apply(EmbeddedCacheManager manager) {
        return new DefaultCacheContainer(manager, this.batcherFactory);
    }

    @Override
    public EmbeddedCacheManager get() {
        GlobalConfiguration config = this.configuration.getValue();
        String defaultCacheName = config.defaultCacheName().orElse(null);
        // We need to create a dummy default configuration if cache has a default cache
        Configuration defaultConfiguration = (defaultCacheName != null) ? new ConfigurationBuilder().build() : null;
        EmbeddedCacheManager manager = new DefaultCacheManager(config, defaultConfiguration, false);
        // Undefine the default cache, if we defined one
        if (defaultCacheName != null) {
            manager.undefineConfiguration(defaultCacheName);
        }
        manager.addListener(this);

        manager.start();
        InfinispanLogger.ROOT_LOGGER.debugf("%s cache container started", this.name);
        return manager;
    }

    @Override
    public void accept(EmbeddedCacheManager manager) {
        manager.stop();
        manager.removeListener(this);
        InfinispanLogger.ROOT_LOGGER.debugf("%s cache container stopped", this.name);
    }

    @Override
    public CacheContainerBuilder configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.aliases.clear();
        for (ModelNode alias : ModelNodes.optionalList(ALIASES.resolveModelAttribute(context, model)).orElse(Collections.emptyList())) {
            this.aliases.add(InfinispanRequirement.CONTAINER.getServiceName(context.getCapabilityServiceSupport(), alias.asString()));
        }
        return this;
    }

    @Override
    public ServiceBuilder<CacheContainer> build(ServiceTarget target) {
        Service<CacheContainer> service = new SuppliedValueService<>(this, this, this);
        ServiceBuilder<CacheContainer> builder = target.addService(this.getServiceName(), service);
        for (ServiceName alias : this.aliases) {
            builder.addAliases(alias);
        }
        return this.configuration.register(builder);
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
