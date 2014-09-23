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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactoryService;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.clustering.msc.InjectedValueServiceBuilder;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
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

    private static void populate(ModelNode source, ModelNode target) throws OperationFailedException {
        CacheContainerResourceDefinition.DEFAULT_CACHE.validateAndSet(source, target);
        // TODO: need to handle list types
        if (source.hasDefined(CacheContainerResourceDefinition.ALIASES.getName())) {
            target.get(CacheContainerResourceDefinition.ALIASES.getName()).set(source.get(CacheContainerResourceDefinition.ALIASES.getName()));
        }
        CacheContainerResourceDefinition.JNDI_NAME.validateAndSet(source, target);
        CacheContainerResourceDefinition.START.validateAndSet(source, target);
        CacheContainerResourceDefinition.LISTENER_EXECUTOR.validateAndSet(source, target);
        CacheContainerResourceDefinition.EVICTION_EXECUTOR.validateAndSet(source, target);
        CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR.validateAndSet(source, target);
        CacheContainerResourceDefinition.MODULE.validateAndSet(source, target);
        CacheContainerResourceDefinition.STATISTICS_ENABLED.validateAndSet(source, target);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        populate(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode localModel, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        // Because we use child resources in a read-only manner to configure the cache container, replace the local model with the full model
        ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        newControllers.addAll(installRuntimeServices(context, operation, model, verificationHandler));
    }

    static Collection<ServiceController<?>> installRuntimeServices(OperationContext context, ModelNode operation, ModelNode containerModel, ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

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
                return Collections.emptyList();
            }
        }

        final ServiceTarget target = context.getServiceTarget();

        // pick up the attribute values from the model
        // make default cache non required (AS7-3488)
        final String defaultCache = ModelNodes.asString(CacheContainerResourceDefinition.DEFAULT_CACHE.resolveModelAttribute(context, containerModel));
        final String jndiName = ModelNodes.asString(CacheContainerResourceDefinition.JNDI_NAME.resolveModelAttribute(context, containerModel));
        final String listenerExecutor = ModelNodes.asString(CacheContainerResourceDefinition.LISTENER_EXECUTOR.resolveModelAttribute(context, containerModel));
        final String evictionExecutor = ModelNodes.asString(CacheContainerResourceDefinition.EVICTION_EXECUTOR.resolveModelAttribute(context, containerModel));
        final String replicationQueueExecutor = ModelNodes.asString(CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR.resolveModelAttribute(context, containerModel));
        final ServiceController.Mode initialMode = StartMode.valueOf(CacheContainerResourceDefinition.START.resolveModelAttribute(context, containerModel).asString()).getMode();
        final boolean statistics = CacheContainerResourceDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, containerModel).asBoolean();

        ServiceName[] aliases = null;
        if (containerModel.hasDefined(CacheContainerResourceDefinition.ALIASES.getName())) {
            List<ModelNode> list = operation.get(CacheContainerResourceDefinition.ALIASES.getName()).asList();
            aliases = new ServiceName[list.size()];
            for (int i = 0; i < list.size(); i++) {
                aliases[i] = EmbeddedCacheManagerService.getServiceName(list.get(i).asString());
            }
        }

        final ModuleIdentifier module = ModelNodes.asModuleIdentifier(CacheContainerResourceDefinition.MODULE.resolveModelAttribute(context, containerModel));

        Transport transportConfig = null;
        String transportExecutor = null;

        Collection<ServiceController<?>> controllers = new LinkedList<>();
        InjectedValueServiceBuilder builder = new InjectedValueServiceBuilder(target);

        if (containerModel.hasDefined(TransportResourceDefinition.PATH.getKey())) {
            ModelNode transport = containerModel.get(TransportResourceDefinition.PATH.getKeyValuePair());
            if (transport.isDefined()) {
                transportConfig = new Transport();

                String channel = ModelNodes.asString(TransportResourceDefinition.CHANNEL.resolveModelAttribute(context, transport));
                long lockTimeout = TransportResourceDefinition.LOCK_TIMEOUT.resolveModelAttribute(context, transport).asLong();
                transportExecutor = ModelNodes.asString(TransportResourceDefinition.EXECUTOR.resolveModelAttribute(context, transport));

                transportConfig.setLockTimeout(lockTimeout);
                transportConfig.setClusterName(name);

                if (!name.equals(channel)) {
                    controllers.add(new BinderServiceBuilder(target).build(ChannelService.createChannelBinding(name), ChannelService.getServiceName(name), Channel.class).install());

                    controllers.add(ChannelService.build(target, name).setInitialMode(ON_DEMAND).install());

                    if (channel == null) {
                        // Transport uses the default channel - we need to find its actual name to locate the appropriate ChannelFactory service
                        PathAddress jgroupsAddress = address.subAddress(0, address.size() - 2).append(JGroupsSubsystemResourceDefinition.PATH);
                        ModelNode jgroupsModel = context.readResourceFromRoot(jgroupsAddress).getModel();
                        channel = ModelNodes.asString(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.resolveModelAttribute(context, jgroupsModel));
                    }

                    controllers.add(new InjectedValueServiceBuilder(target).build(ChannelService.getFactoryServiceName(name), ChannelFactoryService.getServiceName(channel), ChannelFactory.class).install());

                    for (GroupServiceInstaller installer : ServiceLoader.load(ClusteredGroupServiceInstaller.class, ClusteredGroupServiceInstaller.class.getClassLoader())) {
                        log.debugf("Installing %s for cache container %s", installer.getClass().getSimpleName(), name);
                        Iterator<ServiceName> serviceNames = installer.getServiceNames(channel).iterator();
                        for (ServiceName serviceName : installer.getServiceNames(name)) {
                            controllers.add(builder.build(serviceName, serviceNames.next(), Object.class).install());
                        }
                    }
                }
            }
        }
        if (transportConfig == null) {
            for (GroupServiceInstaller installer : ServiceLoader.load(LocalGroupServiceInstaller.class, LocalGroupServiceInstaller.class.getClassLoader())) {
                log.debugf("Installing %s for cache container %s", installer.getClass().getSimpleName(), name);
                controllers.addAll(installer.install(target, name, module));
            }
        }

        // install the cache container configuration service
        controllers.add(installContainerConfigurationService(target, name, defaultCache, statistics, module, transportConfig,
                        transportExecutor, listenerExecutor, evictionExecutor, replicationQueueExecutor, verificationHandler));

        // install a cache container service
        controllers.add(installContainerService(target, name, aliases, transportConfig, initialMode, verificationHandler));

        // install a name service entry for the cache container
        controllers.add(installJndiService(target, name, jndiName, verificationHandler));

        controllers.add(installKeyAffinityServiceFactoryService(target, name, verificationHandler));

        if ((defaultCache != null) && !defaultCache.equals(CacheContainer.DEFAULT_CACHE_ALIAS)) {
            Class<? extends CacheServiceInstaller> installerClass = (transportConfig != null) ? ClusteredCacheServiceInstaller.class : LocalCacheServiceInstaller.class;
            for (CacheServiceInstaller installer : ServiceLoader.load(installerClass, installerClass.getClassLoader())) {
                controllers.addAll(installer.install(target, name, CacheContainer.DEFAULT_CACHE_ALIAS));
            }
        }

        log.debugf("%s cache container installed", name);
        return controllers;
    }

    private static ServiceController<?> installKeyAffinityServiceFactoryService(ServiceTarget target, String containerName, ServiceVerificationHandler verificationHandler) {
        return KeyAffinityServiceFactoryService.build(target, containerName, 10)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }

    private static ServiceController<?> installContainerConfigurationService(ServiceTarget target,
            String containerName, String defaultCache, boolean statistics, ModuleIdentifier module, Transport transportConfig,
            String transportExecutor, String listenerExecutor, String evictionExecutor, String replicationQueueExecutor,
            ServiceVerificationHandler verificationHandler) {

        final ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(containerName);
        final EmbeddedCacheManagerDependencies dependencies = new EmbeddedCacheManagerDependencies(transportConfig);
        final Service<EmbeddedCacheManagerConfiguration> service = new EmbeddedCacheManagerConfigurationService(containerName, defaultCache, statistics, module, dependencies);
        final ServiceBuilder<EmbeddedCacheManagerConfiguration> configBuilder = target.addService(configServiceName, service)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, dependencies.getModuleLoaderInjector())
                .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, dependencies.getMBeanServerInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        // add these dependencies only if we have a transport defined
        if (transportConfig != null) {
            if (transportExecutor != null) {
                addExecutorDependency(configBuilder, transportExecutor, transportConfig.getExecutorInjector());
            }
            configBuilder.addDependency(ChannelService.getServiceName(containerName), Channel.class, transportConfig.getChannelInjector());
            configBuilder.addDependency(ChannelService.getFactoryServiceName(containerName), ChannelFactory.class, transportConfig.getChannelFactoryInjector());
        }

        addExecutorDependency(configBuilder, listenerExecutor, dependencies.getListenerExecutorInjector());
        addScheduledExecutorDependency(configBuilder, evictionExecutor, dependencies.getEvictionExecutorInjector());
        addScheduledExecutorDependency(configBuilder, replicationQueueExecutor, dependencies.getReplicationQueueExecutorInjector());

        return configBuilder.install();
    }

    private static ServiceController<?> installContainerService(ServiceTarget target, String containerName, ServiceName[] aliases, Transport transport, ServiceController.Mode initialMode, ServiceVerificationHandler verificationHandler) {

        final ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName);
        final ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(containerName);
        final InjectedValue<EmbeddedCacheManagerConfiguration> config = new InjectedValue<>();
        final Service<EmbeddedCacheManager> service = new EmbeddedCacheManagerService(config);
        ServiceBuilder<EmbeddedCacheManager> builder = target.addService(containerServiceName, service)
                .addDependency(configServiceName, EmbeddedCacheManagerConfiguration.class, config)
                .addAliases(aliases)
                .setInitialMode(initialMode)
        ;
        if (transport != null) {
            builder.addDependency(ChannelService.getServiceName(containerName));
        }
        return builder.install();
    }

    private static ServiceController<?> installJndiService(ServiceTarget target, String containerName, String jndiName, ServiceVerificationHandler verificationHandler) {

        final ServiceName name = EmbeddedCacheManagerService.getServiceName(containerName);
        final ContextNames.BindInfo binding = createCacheContainerBinding(jndiName, containerName);
        return new BinderServiceBuilder(target).build(binding, name, CacheContainer.class).install();
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

        private Long lockTimeout;
        private String clusterName;

        void setLockTimeout(long lockTimeout) {
            this.lockTimeout = lockTimeout;
        }

        void setClusterName(String clusterName) {
            this.clusterName = clusterName;
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
        public Long getLockTimeout() {
            return this.lockTimeout;
        }

        @Override
        public String getClusterName() {
            return this.clusterName;
        }
    }
}
