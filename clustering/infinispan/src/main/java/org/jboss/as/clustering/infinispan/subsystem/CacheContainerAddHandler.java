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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactoryService;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelInstanceResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsExtension;
import org.jboss.as.clustering.msc.AsynchronousService;
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
import org.jboss.as.controller.operations.common.Util;
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
import org.wildfly.clustering.spi.ClusterServiceInstaller;
import org.wildfly.clustering.spi.LocalServiceInstaller;
import org.wildfly.clustering.spi.ServiceInstaller;

/**
 * @author Paul Ferraro
 * @author Tristan Tarrant
 * @author Richard Achmatowicz
 */
public class CacheContainerAddHandler extends AbstractAddStepHandler {

    private static final Logger log = Logger.getLogger(CacheContainerAddHandler.class.getPackage().getName());

    static ModelNode createOperation(ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) throws OperationFailedException {
        // AS7-3488 make default-cache non required attrinbute
        // target.get(ModelKeys.DEFAULT_CACHE).set(source.get(ModelKeys.DEFAULT_CACHE));

        CacheContainerResourceDefinition.DEFAULT_CACHE.validateAndSet(source, target);
        // TODO: need to handle list types
        if (source.hasDefined(ModelKeys.ALIASES)) {
            target.get(ModelKeys.ALIASES).set(source.get(ModelKeys.ALIASES));
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

        final PathAddress address = getCacheContainerAddressFromOperation(operation);
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
        ModelNode resolvedValue = null;
        // make default cache non required (AS7-3488)
        final String defaultCache = (resolvedValue = CacheContainerResourceDefinition.DEFAULT_CACHE.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null;
        final String jndiName = (resolvedValue = CacheContainerResourceDefinition.JNDI_NAME.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null;
        final String listenerExecutor = (resolvedValue = CacheContainerResourceDefinition.LISTENER_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null;
        final String evictionExecutor = (resolvedValue = CacheContainerResourceDefinition.EVICTION_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null;
        final String replicationQueueExecutor = (resolvedValue = CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null;
        final ServiceController.Mode initialMode = StartMode.valueOf(CacheContainerResourceDefinition.START.resolveModelAttribute(context, containerModel).asString()).getMode();
        final boolean statistics = CacheContainerResourceDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, containerModel).asBoolean();

        ServiceName[] aliases = null;
        if (containerModel.hasDefined(ModelKeys.ALIASES)) {
            List<ModelNode> list = operation.get(ModelKeys.ALIASES).asList();
            aliases = new ServiceName[list.size()];
            for (int i = 0; i < list.size(); i++) {
                aliases[i] = EmbeddedCacheManagerService.getServiceName(list.get(i).asString());
            }
        }

        final ModuleIdentifier moduleId = (resolvedValue = CacheContainerResourceDefinition.MODULE.resolveModelAttribute(context, containerModel)).isDefined() ? ModuleIdentifier.fromString(resolvedValue.asString()) : null;

        // if we have a transport defined, pick up the transport-related attributes and install a channel
        final Transport transportConfig = containerModel.hasDefined(ModelKeys.TRANSPORT) && containerModel.get(ModelKeys.TRANSPORT).hasDefined(ModelKeys.TRANSPORT_NAME) ? new Transport() : null;

        String stack = null;
        String transportExecutor = null;

        Collection<ServiceController<?>> controllers = new LinkedList<>();
        boolean clustered = (transportConfig != null);

        if (clustered) {
            ModelNode transport = containerModel.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);

            if ((resolvedValue = TransportResourceDefinition.STACK.resolveModelAttribute(context, transport)).isDefined()) {
                stack = resolvedValue.asString();
            }
            // if cluster is not defined, use the cache container name as the default
            String clusterName = name;
            if ((resolvedValue = TransportResourceDefinition.CLUSTER.resolveModelAttribute(context, transport)).isDefined()) {
                clusterName = resolvedValue.asString();
            }
            long lockTimeout = TransportResourceDefinition.LOCK_TIMEOUT.resolveModelAttribute(context, transport).asLong();
            if ((resolvedValue = TransportResourceDefinition.EXECUTOR.resolveModelAttribute(context, transport)).isDefined()) {
                transportExecutor = resolvedValue.asString();
            }

            // initialise the Transport
            transportConfig.setClusterName(clusterName);
            transportConfig.setLockTimeout(lockTimeout);

            controllers.addAll(installChannelServices(target, name, stack, verificationHandler));

            // register the protocol metrics by adding a step
            ChannelInstanceResourceDefinition.addChannelProtocolMetricsRegistrationStep(context, clusterName, stack);

            install(target, ClusterServiceInstaller.class, name, moduleId);
        } else {
            install(target, LocalServiceInstaller.class, name, moduleId);
        }

        // install the cache container configuration service
        controllers.add(installContainerConfigurationService(target, name, defaultCache, statistics, moduleId, stack, transportConfig,
                        transportExecutor, listenerExecutor, evictionExecutor, replicationQueueExecutor, verificationHandler));

        // install a cache container service
        controllers.add(installContainerService(target, name, aliases, transportConfig, initialMode, verificationHandler));

        // install a name service entry for the cache container
        controllers.add(installJndiService(target, name, jndiName, verificationHandler));

        controllers.add(installKeyAffinityServiceFactoryService(target, name, verificationHandler));

        controllers.add(installGlobalComponentRegistryService(target, name, transportConfig, verificationHandler));

        log.debugf("%s cache container installed", name);
        return controllers;
    }

    private static <I extends ServiceInstaller> void install(ServiceTarget target, Class<I> installerClass, String group, ModuleIdentifier moduleId) {
        for (I installer: ServiceLoader.load(installerClass, installerClass.getClassLoader())) {
            log.debugf("Installing %s for channel %s", installer.getClass().getSimpleName(), group);
            installer.install(target, group, moduleId);
        }
    }

    static void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = getCacheContainerAddressFromOperation(operation);
        final String containerName = address.getLastElement().getValue();

        // need to remove all container-related services started, in reverse order
        context.removeService(KeyAffinityServiceFactoryService.getServiceName(containerName));

        // remove the BinderService entry
        ModelNode resolvedValue = null;
        final String jndiName = (resolvedValue = CacheContainerResourceDefinition.JNDI_NAME.resolveModelAttribute(context, model)).isDefined() ? resolvedValue.asString() : null;
        context.removeService(createCacheContainerBinding(jndiName, containerName).getBinderServiceName());

        // remove the cache container
        context.removeService(EmbeddedCacheManagerService.getServiceName(containerName));
        context.removeService(EmbeddedCacheManagerConfigurationService.getServiceName(containerName));
        context.removeService(GlobalComponentRegistryService.getServiceName(containerName));

        if (model.hasDefined(ModelKeys.TRANSPORT)) {
            removeServices(context, ClusterServiceInstaller.class, containerName);

            // unregister the protocol metrics by adding a step
            ChannelInstanceResourceDefinition.addChannelProtocolMetricsDeregistrationStep(context, containerName);

            context.removeService(createChannelBinding(containerName).getBinderServiceName());
            context.removeService(ChannelService.getServiceName(containerName));
        } else {
            removeServices(context, LocalServiceInstaller.class, containerName);
        }
    }

    private static <I extends ServiceInstaller> void removeServices(OperationContext context, Class<I> installerClass, String group) {
        for (I installer: ServiceLoader.load(installerClass, installerClass.getClassLoader())) {
            for (ServiceName name: installer.getServiceNames(group)) {
                context.removeService(name);
            }
        }
    }

    private static ServiceController<?> installGlobalComponentRegistryService(ServiceTarget target, String containerName, Transport transport, ServiceVerificationHandler verificationHandler) {
        InjectedValue<CacheContainer> container = new InjectedValue<>();
        ServiceBuilder<?> builder = AsynchronousService.addService(target, GlobalComponentRegistryService.getServiceName(containerName), new GlobalComponentRegistryService(container))
                .addDependency(EmbeddedCacheManagerService.getServiceName(containerName), CacheContainer.class, container)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
        if (transport != null) {
            builder.addDependency(ChannelService.getServiceName(containerName));
        }
        return builder.install();
    }

    private static ServiceController<?> installKeyAffinityServiceFactoryService(ServiceTarget target, String containerName, ServiceVerificationHandler verificationHandler) {
        return AsynchronousService.addService(target, KeyAffinityServiceFactoryService.getServiceName(containerName), new KeyAffinityServiceFactoryService(10), false, true)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }

    private static Collection<ServiceController<?>> installChannelServices(ServiceTarget target, String containerName, String stack, ServiceVerificationHandler verificationHandler) {

        ContextNames.BindInfo bindInfo = createChannelBinding(containerName);
        ServiceName name = ChannelService.getServiceName(containerName);
        ServiceController<?> binderService = new BinderServiceBuilder(target).build(bindInfo, name, Channel.class).install();

        ServiceController<?> channelService = ChannelService.build(target, containerName, stack)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();

        return Arrays.asList(binderService, channelService);
    }

    private static PathAddress getCacheContainerAddressFromOperation(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(OP_ADDR));
    }

    private static ServiceController<?> installContainerConfigurationService(ServiceTarget target,
            String containerName, String defaultCache, boolean statistics, ModuleIdentifier moduleId, String stack, Transport transportConfig,
            String transportExecutor, String listenerExecutor, String evictionExecutor, String replicationQueueExecutor,
            ServiceVerificationHandler verificationHandler) {

        final ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(containerName);
        final EmbeddedCacheManagerDependencies dependencies = new EmbeddedCacheManagerDependencies(transportConfig);
        final Service<EmbeddedCacheManagerConfiguration> service = new EmbeddedCacheManagerConfigurationService(containerName, defaultCache, statistics, moduleId, dependencies);
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
            configBuilder.addDependency(ChannelFactoryService.getServiceName(stack), ChannelFactory.class, transportConfig.getChannelFactoryInjector());
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

    private static ContextNames.BindInfo createCacheContainerBinding(String jndiName, String container) {
        JndiName name = (jndiName != null) ? JndiNameFactory.parse(jndiName) : JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, InfinispanExtension.SUBSYSTEM_NAME, "container", container);
        return ContextNames.bindInfoFor(name.getAbsoluteName());
    }

    private static ContextNames.BindInfo createChannelBinding(String channel) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, JGroupsExtension.SUBSYSTEM_NAME, "channel", channel).getAbsoluteName());
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
