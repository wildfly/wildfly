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

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.*;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.BatcherFactory;
import org.jboss.as.clustering.infinispan.DefaultCacheContainer;
import org.jboss.as.clustering.infinispan.InfinispanBatcherFactory;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.SuppliedValueService;

/**
 * @author Paul Ferraro
 */
@Listener
public class CacheContainerBuilder implements ResourceServiceBuilder<CacheContainer> {

    private final InjectedValue<GlobalConfiguration> configuration = new InjectedValue<>();
    private final List<String> aliases = new LinkedList<>();
    private final String name;
    private final BatcherFactory batcherFactory = new InfinispanBatcherFactory();

    private volatile String defaultCache;

    public CacheContainerBuilder(String name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheContainerServiceName.CACHE_CONTAINER.getServiceName(this.name);
    }

    @Override
    public Builder<CacheContainer> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.aliases.clear();
        this.aliases.addAll(ModelNodes.asStringList(ALIASES.getDefinition().resolveModelAttribute(context, model)));
        return this;
    }

    @Override
    public ServiceBuilder<CacheContainer> build(ServiceTarget target) {
        Function<EmbeddedCacheManager, CacheContainer> mapper = manager -> new DefaultCacheContainer(this.name, manager, this.defaultCache, this.batcherFactory);
        Supplier<EmbeddedCacheManager> supplier = () -> {
            GlobalConfiguration config = this.configuration.getValue();
            EmbeddedCacheManager manager = new DefaultCacheManager(config, null, false);
            manager.addListener(this);
            manager.start();
            InfinispanLogger.ROOT_LOGGER.debugf("%s cache container started", this.name);
            manager.getGlobalComponentRegistry().start();
            return manager;
        };
        Consumer<EmbeddedCacheManager> destroyer = manager -> {
            if (manager.getStatus().allowInvocations()) {
                manager.stop();
                InfinispanLogger.ROOT_LOGGER.debugf("%s cache container stopped", this.name);
            }
            manager.removeListener(this);
        };
        Service<CacheContainer> service = new SuppliedValueService<>(mapper, supplier, destroyer);
        ServiceBuilder<CacheContainer> builder = target.addService(this.getServiceName(), service)
                .addDependency(CacheContainerServiceName.CONFIGURATION.getServiceName(this.name), GlobalConfiguration.class, this.configuration)
        ;
        this.aliases.forEach(alias -> builder.addAliases(CacheContainerServiceName.CACHE_CONTAINER.getServiceName(alias)));
        return builder.setInitialMode(ServiceController.Mode.PASSIVE);
    }

    CacheContainerBuilder setDefaultCache(String defaultCache) {
        this.defaultCache = defaultCache;
        return this;
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
