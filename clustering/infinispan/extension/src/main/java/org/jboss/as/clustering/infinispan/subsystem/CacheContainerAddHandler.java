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

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactoryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsBindingFactory;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.server.Services;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.service.ChannelBuilder;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceName;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.spi.CacheServiceInstaller;
import org.wildfly.clustering.spi.ClusteredGroupServiceInstaller;
import org.wildfly.clustering.spi.ClusteredCacheServiceInstaller;
import org.wildfly.clustering.spi.GroupServiceInstaller;
import org.wildfly.clustering.spi.LocalCacheServiceInstaller;
import org.wildfly.clustering.spi.LocalGroupServiceInstaller;

/**
 * @author Paul Ferraro
 * @author Tristan Tarrant
 * @author Richard Achmatowicz
 */
public class CacheContainerAddHandler extends AbstractAddStepHandler {

    private static final Logger log = Logger.getLogger(CacheContainerAddHandler.class.getPackage().getName());

    CacheContainerAddHandler() {
        super(CacheContainerResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // Because we use child resources in a read-only manner to configure the cache container, replace the local model with the full model
        installRuntimeServices(context, operation, Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
    }

    static void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress address = Operations.getPathAddress(operation);
        String name = address.getLastElement().getValue();

        // Handle case where ejb subsystem has already installed services for this cache-container
        // This can happen if the ejb cache-container is added to a running server
        if (context.getProcessType().isServer() && !context.isBooting() && name.equals("ejb")) {
            Resource rootResource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
            PathElement ejbPath = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "ejb3");
            if (rootResource.hasChild(ejbPath) && rootResource.getChild(ejbPath).hasChild(PathElement.pathElement("service", "remote"))) {
                // Following restart, these services will be installed by this handler, rather than the ejb remote handler
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        context.reloadRequired();
                        context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                    }
                }, OperationContext.Stage.RUNTIME);
                return;
            }
        }

        ServiceTarget target = context.getServiceTarget();

        // pick up the attribute values from the model
        // make default cache non required (AS7-3488)
        String defaultCache = ModelNodes.asString(CacheContainerResourceDefinition.DEFAULT_CACHE.resolveModelAttribute(context, model));
        String jndiName = ModelNodes.asString(CacheContainerResourceDefinition.JNDI_NAME.resolveModelAttribute(context, model));
        String listenerExecutor = ModelNodes.asString(CacheContainerResourceDefinition.LISTENER_EXECUTOR.resolveModelAttribute(context, model));
        String evictionExecutor = ModelNodes.asString(CacheContainerResourceDefinition.EVICTION_EXECUTOR.resolveModelAttribute(context, model));
        String replicationQueueExecutor = ModelNodes.asString(CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR.resolveModelAttribute(context, model));
        ServiceController.Mode initialMode = StartMode.valueOf(CacheContainerResourceDefinition.START.resolveModelAttribute(context, model).asString()).getMode();
        boolean statistics = CacheContainerResourceDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        ServiceName[] aliases = null;
        if (model.hasDefined(CacheContainerResourceDefinition.ALIASES.getName())) {
            List<ModelNode> list = operation.get(CacheContainerResourceDefinition.ALIASES.getName()).asList();
            aliases = new ServiceName[list.size()];
            for (int i = 0; i < list.size(); i++) {
                aliases[i] = EmbeddedCacheManagerService.getServiceName(list.get(i).asString());
            }
        }

        final ModuleIdentifier module = ModelNodes.asModuleIdentifier(CacheContainerResourceDefinition.MODULE.resolveModelAttribute(context, model));

        Transport transportConfig = null;
        String transportExecutor = null;

        if (model.hasDefined(TransportResourceDefinition.PATH.getKey())) {
            ModelNode transport = model.get(TransportResourceDefinition.PATH.getKeyValuePair());
            if (transport.isDefined()) {
                transportConfig = new Transport(TransportResourceDefinition.LOCK_TIMEOUT.resolveModelAttribute(context, transport).asLong());

                String channel = ModelNodes.asString(TransportResourceDefinition.CHANNEL.resolveModelAttribute(context, transport));
                transportExecutor = ModelNodes.asString(TransportResourceDefinition.EXECUTOR.resolveModelAttribute(context, transport));

                if (channel == null) {
                    // Transport uses the default channel - we need to find its actual name to locate the appropriate ChannelFactory service
                    PathAddress jgroupsAddress = address.subAddress(0, address.size() - 2).append(JGroupsSubsystemResourceDefinition.PATH);
                    ModelNode jgroupsModel = context.readResourceFromRoot(jgroupsAddress, false).getModel();
                    channel = ModelNodes.asString(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.resolveModelAttribute(context, jgroupsModel));
                }

                if (!name.equals(channel)) {
                    new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(name), ChannelServiceName.CHANNEL.getServiceName(name), Channel.class).build(target).install();

                    new ChannelBuilder(name).build(target).install();

                    new AliasServiceBuilder<>(ChannelServiceName.FACTORY.getServiceName(name), ChannelFactoryService.getServiceName(channel), ChannelFactory.class).build(target).install();

                    for (GroupServiceInstaller installer : ServiceLoader.load(ClusteredGroupServiceInstaller.class, ClusteredGroupServiceInstaller.class.getClassLoader())) {
                        log.debugf("Installing %s for cache container %s", installer.getClass().getSimpleName(), name);
                        Iterator<ServiceName> names = installer.getServiceNames(channel).iterator();
                        for (ServiceName serviceName : installer.getServiceNames(name)) {
                            new AliasServiceBuilder<>(serviceName, names.next(), Object.class).build(target).install();
                        }
                    }
                }
            }
        }
        if (transportConfig == null) {
            for (GroupServiceInstaller installer : ServiceLoader.load(LocalGroupServiceInstaller.class, LocalGroupServiceInstaller.class.getClassLoader())) {
                log.debugf("Installing %s for cache container %s", installer.getClass().getSimpleName(), name);
                installer.install(target, name, module);
            }
        }

        // Install cache container configuration service
        ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(name);
        EmbeddedCacheManagerDependencies dependencies = new EmbeddedCacheManagerDependencies(transportConfig);
        ServiceBuilder<EmbeddedCacheManagerConfiguration> configBuilder = target.addService(configServiceName, new EmbeddedCacheManagerConfigurationService(name, defaultCache, statistics, module, dependencies))
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, dependencies.getModuleLoaderInjector())
                .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, dependencies.getMBeanServerInjector())
        ;
        if (transportConfig != null) {
            if (transportExecutor != null) {
                addExecutorDependency(configBuilder, transportExecutor, transportConfig.getExecutorInjector());
            }
            configBuilder.addDependency(ChannelServiceName.CHANNEL.getServiceName(name), Channel.class, transportConfig.getChannelInjector());
            configBuilder.addDependency(ChannelServiceName.FACTORY.getServiceName(name), ChannelFactory.class, transportConfig.getChannelFactoryInjector());
        }
        addExecutorDependency(configBuilder, listenerExecutor, dependencies.getListenerExecutorInjector());
        addScheduledExecutorDependency(configBuilder, evictionExecutor, dependencies.getEvictionExecutorInjector());
        addScheduledExecutorDependency(configBuilder, replicationQueueExecutor, dependencies.getReplicationQueueExecutorInjector());

        configBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        // Install cache container service
        ServiceBuilder<EmbeddedCacheManager> managerBuilder = EmbeddedCacheManagerService.build(target, name)
                .addAliases(aliases)
        ;
        if (transportConfig != null) {
            managerBuilder.addDependency(ChannelServiceName.CHANNEL.getServiceName(name));
        }
        managerBuilder.setInitialMode(initialMode).install();

        // Install cache container jndi binding
        ServiceName serviceName = EmbeddedCacheManagerService.getServiceName(name);
        ContextNames.BindInfo binding = createCacheContainerBinding(jndiName, name);
        new BinderServiceBuilder<>(binding, serviceName, CacheContainer.class).build(target).install();

        // Install key affinity service factory
        KeyAffinityServiceFactoryService.build(target, name, 10).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        if ((defaultCache != null) && !defaultCache.equals(CacheContainer.DEFAULT_CACHE_ALIAS)) {
            Class<? extends CacheServiceInstaller> installerClass = (transportConfig != null) ? ClusteredCacheServiceInstaller.class : LocalCacheServiceInstaller.class;
            for (CacheServiceInstaller installer : ServiceLoader.load(installerClass, installerClass.getClassLoader())) {
                installer.install(target, name, CacheContainer.DEFAULT_CACHE_ALIAS);
            }
        }
    }

    static void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String name = Operations.getPathAddress(operation).getLastElement().getValue();

        // need to remove all container-related services started, in reverse order
        context.removeService(KeyAffinityServiceFactoryService.getServiceName(name));

        // remove the BinderService entry
        String jndiName = ModelNodes.asString(CacheContainerResourceDefinition.JNDI_NAME.resolveModelAttribute(context, model));
        context.removeService(CacheContainerAddHandler.createCacheContainerBinding(jndiName, name).getBinderServiceName());

        // remove the cache container
        context.removeService(EmbeddedCacheManagerService.getServiceName(name));
        context.removeService(EmbeddedCacheManagerConfigurationService.getServiceName(name));

        if (model.hasDefined(TransportResourceDefinition.PATH.getKey())) {
            removeServices(context, ClusteredGroupServiceInstaller.class, name);

            context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());
            context.removeService(ChannelServiceName.CHANNEL.getServiceName(name));
            context.removeService(ChannelServiceName.FACTORY.getServiceName(name));
        } else {
            removeServices(context, LocalGroupServiceInstaller.class, name);
        }

        String defaultCache = ModelNodes.asString(CacheContainerResourceDefinition.DEFAULT_CACHE.resolveModelAttribute(context, model));

        if ((defaultCache != null) && !defaultCache.equals(CacheContainer.DEFAULT_CACHE_ALIAS)) {
            Class<? extends CacheServiceInstaller> installerClass = model.hasDefined(TransportResourceDefinition.PATH.getKey()) ? ClusteredCacheServiceInstaller.class : LocalCacheServiceInstaller.class;
            for (CacheServiceInstaller installer : ServiceLoader.load(installerClass, installerClass.getClassLoader())) {
                for (ServiceName serviceName : installer.getServiceNames(name, CacheContainer.DEFAULT_CACHE_ALIAS)) {
                    context.removeService(serviceName);
                }
            }
        }
    }

    private static <I extends GroupServiceInstaller> void removeServices(OperationContext context, Class<I> installerClass, String group) {
        for (I installer: ServiceLoader.load(installerClass, installerClass.getClassLoader())) {
            for (ServiceName name: installer.getServiceNames(group)) {
                context.removeService(name);
            }
        }
    }

    private static void addExecutorDependency(ServiceBuilder<EmbeddedCacheManagerConfiguration> builder, String executor, Injector<Executor> injector) {
        if (executor != null) {
            builder.addDependency(ThreadsServices.executorName(executor), Executor.class, injector);
        }
    }

    private static void addScheduledExecutorDependency(ServiceBuilder<EmbeddedCacheManagerConfiguration> builder, String executor, Injector<ScheduledExecutorService> injector) {
        if (executor != null) {
            builder.addDependency(ThreadsServices.executorName(executor), ScheduledExecutorService.class, injector);
        }
    }

    static ContextNames.BindInfo createCacheContainerBinding(String jndiName, String container) {
        JndiName name = (jndiName != null) ? JndiNameFactory.parse(jndiName) : JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, InfinispanExtension.SUBSYSTEM_NAME, "container", container);
        return ContextNames.bindInfoFor(name.getAbsoluteName());
    }

    static class EmbeddedCacheManagerDependencies implements EmbeddedCacheManagerConfigurationService.Dependencies {
        private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<>();
        private final InjectedValue<Executor> listenerExecutor = new InjectedValue<>();
        private final InjectedValue<ScheduledExecutorService> evictionExecutor = new InjectedValue<>();
        private final InjectedValue<ScheduledExecutorService> replicationQueueExecutor = new InjectedValue<>();
        private final EmbeddedCacheManagerConfigurationService.TransportConfiguration transport;
        private final InjectedValue<ModuleLoader> moduleLoader = new InjectedValue<>();

        EmbeddedCacheManagerDependencies(EmbeddedCacheManagerConfigurationService.TransportConfiguration transport) {
            this.transport = transport;
        }

        Injector<MBeanServer> getMBeanServerInjector() {
            return this.mbeanServer;
        }

        Injector<Executor> getListenerExecutorInjector() {
            return this.listenerExecutor;
        }

        Injector<ScheduledExecutorService> getEvictionExecutorInjector() {
            return this.evictionExecutor;
        }

        Injector<ScheduledExecutorService> getReplicationQueueExecutorInjector() {
            return this.replicationQueueExecutor;
        }

        Injector<ModuleLoader> getModuleLoaderInjector() {
            return this.moduleLoader;
        }

        @Override
        public EmbeddedCacheManagerConfigurationService.TransportConfiguration getTransportConfiguration() {
            return this.transport;
        }

        @Override
        public MBeanServer getMBeanServer() {
            return this.mbeanServer.getOptionalValue();
        }

        @Override
        public Executor getListenerExecutor() {
            return this.listenerExecutor.getOptionalValue();
        }

        @Override
        public ScheduledExecutorService getEvictionExecutor() {
            return this.evictionExecutor.getOptionalValue();
        }

        @Override
        public ScheduledExecutorService getReplicationQueueExecutor() {
            return this.replicationQueueExecutor.getOptionalValue();
        }

        @Override
        public ModuleLoader getModuleLoader() {
            return this.moduleLoader.getValue();
        }
    }

    static class Transport implements EmbeddedCacheManagerConfigurationService.TransportConfiguration {
        private final InjectedValue<ChannelFactory> channelFactory = new InjectedValue<>();
        private final InjectedValue<Channel> channel = new InjectedValue<>();
        private final InjectedValue<Executor> executor = new InjectedValue<>();
        private final long lockTimeout;

        Transport(long lockTimeout) {
            this.lockTimeout = lockTimeout;
        }

        Injector<ChannelFactory> getChannelFactoryInjector() {
            return this.channelFactory;
        }

        Injector<Executor> getExecutorInjector() {
            return this.executor;
        }

        Injector<Channel> getChannelInjector() {
            return this.channel;
        }

        @Override
        public Channel getChannel() {
            return this.channel.getValue();
        }

        @Override
        public ChannelFactory getChannelFactory() {
            return this.channelFactory.getValue();
        }

        @Override
        public Executor getExecutor() {
            return this.executor.getOptionalValue();
        }

        @Override
        public long getLockTimeout() {
            return this.lockTimeout;
        }
    }
}
