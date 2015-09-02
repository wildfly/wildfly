/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.ServiceLoader;

import javax.management.MBeanServer;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.marshall.core.Ids;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.infinispan.MBeanServerProvider;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.infinispan.spi.marshalling.AdvancedExternalizerAdapter;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public class GlobalConfigurationBuilder implements ResourceServiceBuilder<GlobalConfiguration>, Value<GlobalConfiguration> {

    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final InjectedValue<MBeanServer> server = new InjectedValue<>();
    private final InjectedValue<TransportConfiguration> transport = new InjectedValue<>();
    private final InjectedValue<ThreadPoolConfiguration> asyncOperationsThreadPool = new InjectedValue<>();
    private final InjectedValue<ThreadPoolConfiguration> expirationThreadPool = new InjectedValue<>();
    private final InjectedValue<ThreadPoolConfiguration> listenerThreadPool = new InjectedValue<>();
    private final InjectedValue<ThreadPoolConfiguration> persistenceThreadPool = new InjectedValue<>();
    private final InjectedValue<ThreadPoolConfiguration> remoteCommandThreadPool = new InjectedValue<>();
    private final InjectedValue<ThreadPoolConfiguration> stateTransferThreadPool = new InjectedValue<>();
    private final InjectedValue<ThreadPoolConfiguration> transportThreadPool = new InjectedValue<>();
    private final String name;

    private volatile boolean statisticsEnabled;
    private volatile ModuleIdentifier module;

    GlobalConfigurationBuilder(String name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheContainerServiceName.CONFIGURATION.getServiceName(this.name);
    }

    @Override
    public Builder<GlobalConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.module = ModelNodes.asModuleIdentifier(MODULE.getDefinition().resolveModelAttribute(context, model));
        this.statisticsEnabled = STATISTICS_ENABLED.getDefinition().resolveModelAttribute(context, model).asBoolean();
        return this;
    }

    @Override
    public GlobalConfiguration getValue() {
        org.infinispan.configuration.global.GlobalConfigurationBuilder builder = new org.infinispan.configuration.global.GlobalConfigurationBuilder();
        TransportConfiguration transport = this.transport.getValue();
        // This fails due to ISPN-4755 !!
        // this.builder.transport().read(this.transport.getValue());
        // Workaround this by copying relevant fields individually
        builder.transport().transport(transport.transport())
                .distributedSyncTimeout(transport.distributedSyncTimeout())
                .clusterName(transport.clusterName())
                .machineId(transport.machineId())
                .rackId(transport.rackId())
                .siteId(transport.siteId())
        ;

        ModuleLoader moduleLoader = this.loader.getValue();
        builder.serialization().classResolver(ModularClassResolver.getInstance(moduleLoader));
        try {
            ClassLoader loader = moduleLoader.loadModule(this.module).getClassLoader();
            builder.classLoader(loader);
            int id = Ids.MAX_ID;
            for (Externalizer<?> externalizer: ServiceLoader.load(Externalizer.class, loader)) {
                InfinispanLogger.ROOT_LOGGER.debugf("Cache container %s will use an externalizer for %s", this.name, externalizer.getTargetClass().getName());
                builder.serialization().addAdvancedExternalizer(id++, new AdvancedExternalizerAdapter<>(externalizer));
            }
        } catch (ModuleLoadException e) {
            throw new IllegalStateException(e);
        }

        builder.transport().transportThreadPool().read(this.transportThreadPool.getValue());
        builder.transport().remoteCommandThreadPool().read(this.remoteCommandThreadPool.getValue());

        builder.asyncThreadPool().read(this.asyncOperationsThreadPool.getValue());
        builder.expirationThreadPool().read(this.expirationThreadPool.getValue());
        builder.listenerThreadPool().read(this.listenerThreadPool.getValue());
        builder.stateTransferThreadPool().read(this.stateTransferThreadPool.getValue());
        builder.persistenceThreadPool().read(this.persistenceThreadPool.getValue());

        builder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);
        builder.globalJmxStatistics()
                .enabled(this.statisticsEnabled)
                .cacheManagerName(this.name)
                .mBeanServerLookup(new MBeanServerProvider(this.server.getValue()))
                .jmxDomain(CacheContainerServiceName.CACHE_CONTAINER.getServiceName(CacheServiceName.DEFAULT_CACHE).getParent().getCanonicalName())
                .allowDuplicateDomains(true);

        return builder.build();
    }

    @Override
    public ServiceBuilder<GlobalConfiguration> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, this.server)
                .addDependency(CacheContainerComponent.TRANSPORT.getServiceName(this.name), TransportConfiguration.class, this.transport)
                .addDependency(ThreadPoolResourceDefinition.ASYNC_OPERATIONS.getServiceName(this.name), ThreadPoolConfiguration.class, this.asyncOperationsThreadPool)
                .addDependency(ScheduledThreadPoolResourceDefinition.EXPIRATION.getServiceName(this.name), ThreadPoolConfiguration.class, this.expirationThreadPool)
                .addDependency(ThreadPoolResourceDefinition.LISTENER.getServiceName(this.name), ThreadPoolConfiguration.class, this.listenerThreadPool)
                .addDependency(ThreadPoolResourceDefinition.STATE_TRANSFER.getServiceName(this.name), ThreadPoolConfiguration.class, this.stateTransferThreadPool)
                .addDependency(ThreadPoolResourceDefinition.PERSISTENCE.getServiceName(this.name), ThreadPoolConfiguration.class, this.persistenceThreadPool)
                .addDependency(ThreadPoolResourceDefinition.REMOTE_COMMAND.getServiceName(this.name), ThreadPoolConfiguration.class, this.remoteCommandThreadPool)
                .addDependency(ThreadPoolResourceDefinition.TRANSPORT.getServiceName(this.name), ThreadPoolConfiguration.class, this.transportThreadPool)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }
}
