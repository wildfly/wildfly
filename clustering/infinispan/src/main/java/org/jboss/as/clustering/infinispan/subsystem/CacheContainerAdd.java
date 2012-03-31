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

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;

import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
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

/**
 * @author Paul Ferraro
 * @author Tristan Tarrant
 * @author Richard Achmatowicz
 */
public class CacheContainerAdd extends AbstractAddStepHandler {

    private static final Logger log = Logger.getLogger(CacheContainerAdd.class.getPackage().getName());

    public static final CacheContainerAdd INSTANCE = new CacheContainerAdd();

    static ModelNode createOperation(ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) throws OperationFailedException {
        // AS7-3488 make default-cache non required attrinbute
        // target.get(ModelKeys.DEFAULT_CACHE).set(source.get(ModelKeys.DEFAULT_CACHE));

        CommonAttributes.DEFAULT_CACHE.validateAndSet(source, target);
        // TODO: need to handle list types
        if (source.hasDefined(ModelKeys.ALIASES)) {
            target.get(ModelKeys.ALIASES).set(source.get(ModelKeys.ALIASES));
        }
        CommonAttributes.JNDI_NAME.validateAndSet(source, target);
        CommonAttributes.START.validateAndSet(source, target);
        CommonAttributes.LISTENER_EXECUTOR.validateAndSet(source, target);
        CommonAttributes.EVICTION_EXECUTOR.validateAndSet(source, target);
        CommonAttributes.REPLICATION_QUEUE_EXECUTOR.validateAndSet(source, target);
        CommonAttributes.CACHE_CONTAINER_MODULE.validateAndSet(source, target);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        populate(operation, model);
    }
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        // Because we use child resources in a read-only manner to configure the cache container, replace the local model with the full model
        model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        installRuntimeServices(context, operation, model, verificationHandler, newControllers) ;
    }

    protected void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode containerModel, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        final PathAddress address = getCacheContainerAddressFromOperation(operation);
        final String name = address.getLastElement().getValue();
        final ServiceTarget target = context.getServiceTarget();

        // pick up the attribute values from the model
        ModelNode resolvedValue = null ;
        // make default cache non required (AS7-3488)
        final String defaultCache = (resolvedValue = CommonAttributes.DEFAULT_CACHE.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final String jndiNameString = (resolvedValue = CommonAttributes.JNDI_NAME.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final String listenerExecutor = (resolvedValue = CommonAttributes.LISTENER_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final String evictionExecutor = (resolvedValue = CommonAttributes.EVICTION_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final String replicationQueueExecutor = (resolvedValue = CommonAttributes.REPLICATION_QUEUE_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final ServiceController.Mode initialMode = StartMode.valueOf(CommonAttributes.START.resolveModelAttribute(context, containerModel).asString()).getMode();

        ServiceName[] aliases = null;
        if (containerModel.hasDefined(ModelKeys.ALIASES)) {
            List<ModelNode> list = operation.get(ModelKeys.ALIASES).asList();
            aliases = new ServiceName[list.size()];
            for (int i = 0; i < list.size(); i++) {
                aliases[i] = EmbeddedCacheManagerService.getServiceName(list.get(i).asString());
            }
        }

        final ModuleIdentifier moduleId = (resolvedValue = CommonAttributes.CACHE_CONTAINER_MODULE.resolveModelAttribute(context, containerModel)).isDefined() ? ModuleIdentifier.fromString(resolvedValue.asString()) : null;

        final boolean hasTransport = containerModel.hasDefined(ModelKeys.TRANSPORT) && containerModel.get(ModelKeys.TRANSPORT).hasDefined(ModelKeys.TRANSPORT_NAME);

        // if we have a transport defined, pick up the transport-related attributes and install a channel
        String stack = null ;
        String cluster = null ;
        long lockTimeout = 0;
        String transportExecutor = null ;
        Transport transportConfig = null ;

        if (hasTransport) {
            ModelNode transport = containerModel.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);

            stack = (resolvedValue = CommonAttributes.STACK.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null ;
            // if cluster is not defined, use the cache container name as the default
            cluster = (resolvedValue = CommonAttributes.CLUSTER.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : name ;
            lockTimeout = CommonAttributes.LOCK_TIMEOUT.resolveModelAttribute(context, transport).asLong();
            transportExecutor = (resolvedValue = CommonAttributes.EXECUTOR.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null ;

            // initialise the Transport
            transportConfig = new Transport() ;
            transportConfig.setLockTimeout(lockTimeout);

            // install a name service entry for the cache container
            ServiceController<Channel> csController =
                    installChannelService(target, name, cluster, stack, verificationHandler);
            if (newControllers != null) {
                newControllers.add(csController);
            }
        }

        // install the cache container configuration service
        ServiceController<EmbeddedCacheManagerConfiguration> ccsController =
                installContainerConfigurationService(target, name, defaultCache, moduleId, transportConfig,
                        transportExecutor, listenerExecutor, evictionExecutor, replicationQueueExecutor, verificationHandler);
        if (newControllers != null) {
            newControllers.add(ccsController);
        }

        // install a cache container service
        ServiceController<EmbeddedCacheManager> ccController =
                installContainerService(target, name, aliases, initialMode, verificationHandler);
        if (newControllers != null) {
            newControllers.add(ccController);
        }

        // install a name service entry for the cache container
        ServiceController<ManagedReferenceFactory> jsController =
                installJndiService(target, name, jndiNameString, verificationHandler);
        if (newControllers != null) {
            newControllers.add(jsController);
        }
        log.debugf("%s cache container installed", name);
     }

    protected void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = getCacheContainerAddressFromOperation(operation);
        final String containerName = address.getLastElement().getValue();

        // need to remove all container-related services started, in reverse order
        // remove the BinderService entry
        ModelNode resolvedValue = null;
        final String jndiNameString = (resolvedValue = CommonAttributes.JNDI_NAME.resolveModelAttribute(context, model)).isDefined() ? resolvedValue.asString() : null;
        final String jndiName = InfinispanJndiName.createCacheContainerJndiNameOrDefault(jndiNameString, containerName);
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        context.removeService(bindInfo.getBinderServiceName()) ;

        // remove the cache container
        context.removeService(EmbeddedCacheManagerService.getServiceName(containerName));
        context.removeService(EmbeddedCacheManagerConfigurationService.getServiceName(containerName));

        // check if a channel was installed
        ServiceName channelServiceName = ChannelService.getServiceName(containerName) ;
        ServiceController<?> channelServiceController = context.getServiceRegistry(false).getService(channelServiceName);
        if (channelServiceController != null) {
            context.removeService(channelServiceName);
        }
    }

    protected ServiceController<Channel> installChannelService(ServiceTarget target,
            String containerName, String cluster, String stack,
            ServiceVerificationHandler verificationHandler) {

        ServiceName channelServiceName = ChannelService.getServiceName(containerName);

        InjectedValue<ChannelFactory> channelFactory = new InjectedValue<ChannelFactory>();
        ServiceBuilder<Channel> channelBuilder = target.addService(channelServiceName, new ChannelService(cluster, channelFactory))
                .addDependency(ChannelFactoryService.getServiceName(stack), ChannelFactory.class, channelFactory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        return channelBuilder.install();
    }

    protected PathAddress getCacheContainerAddressFromOperation(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(OP_ADDR)) ;
    }

    protected ServiceController<EmbeddedCacheManagerConfiguration> installContainerConfigurationService(ServiceTarget target,
            String containerName, String defaultCache, ModuleIdentifier moduleId, Transport transportConfig,
            String transportExecutor, String listenerExecutor, String evictionExecutor, String replicationQueueExecutor,
            ServiceVerificationHandler verificationHandler) {

        ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(containerName);

        EmbeddedCacheManagerDependencies dependencies = new EmbeddedCacheManagerDependencies(transportConfig);

        ServiceBuilder<EmbeddedCacheManagerConfiguration> configBuilder = target.addService(configServiceName, new EmbeddedCacheManagerConfigurationService(containerName, defaultCache, moduleId, dependencies))
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, dependencies.getModuleLoaderInjector())
                .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, dependencies.getMBeanServerInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        // add these dependencies only if we have a transport defined
        if (transportConfig != null) {
            if (transportExecutor != null)
                addExecutorDependency(configBuilder, transportExecutor, transportConfig.getExecutorInjector());

            // AS7-1751 decouple channel service name (name) and name used to create channel id (cluster)
            ServiceName channelServiceName = ChannelService.getServiceName(containerName);
            configBuilder.addDependency(channelServiceName, Channel.class, transportConfig.getChannelInjector());
        }

        addExecutorDependency(configBuilder, listenerExecutor, dependencies.getListenerExecutorInjector());
        addScheduledExecutorDependency(configBuilder, evictionExecutor, dependencies.getEvictionExecutorInjector());
        addScheduledExecutorDependency(configBuilder, replicationQueueExecutor, dependencies.getReplicationQueueExecutorInjector());

        return configBuilder.install();
    }

    protected ServiceController<EmbeddedCacheManager> installContainerService(ServiceTarget target,
                                                                            String containerName, ServiceName[] aliases,
                                                                            ServiceController.Mode initialMode,
                                                                            ServiceVerificationHandler verificationHandler) {

        ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName);
        ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(containerName);

        InjectedValue<EmbeddedCacheManagerConfiguration> config = new InjectedValue<EmbeddedCacheManagerConfiguration>();
        ServiceBuilder<EmbeddedCacheManager> containerBuilder = target.addService(containerServiceName, new EmbeddedCacheManagerService(config))
                .addDependency(configServiceName, EmbeddedCacheManagerConfiguration.class, config)
                .addAliases(aliases)
                .setInitialMode(initialMode)
        ;
        return containerBuilder.install();
    }

    protected ServiceController<ManagedReferenceFactory> installJndiService(ServiceTarget target,
                                                                            String containerName, String jndiNameString,
                                                                            ServiceVerificationHandler verificationHandler) {

        String jndiName = InfinispanJndiName.createCacheContainerJndiNameOrDefault(jndiNameString, containerName);

        ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName);
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<ManagedReferenceFactory> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName))
                .addDependency(containerServiceName, CacheContainer.class, new ManagedReferenceInjector<CacheContainer>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE);
        return binderBuilder.install();
    }


    private void addExecutorDependency(ServiceBuilder<EmbeddedCacheManagerConfiguration> builder, String executor, Injector<Executor> injector) {
        if (executor != null) {
            builder.addDependency(ThreadsServices.executorName(executor), Executor.class, injector);
        }
    }

    private void addScheduledExecutorDependency(ServiceBuilder<EmbeddedCacheManagerConfiguration> builder, String executor, Injector<ScheduledExecutorService> injector) {
        if (executor != null) {
            builder.addDependency(ThreadsServices.executorName(executor), ScheduledExecutorService.class, injector);
        }
    }

    static class EmbeddedCacheManagerDependencies implements EmbeddedCacheManagerConfigurationService.Dependencies {
        private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
        private final InjectedValue<Executor> listenerExecutor = new InjectedValue<Executor>();
        private final InjectedValue<ScheduledExecutorService> evictionExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ScheduledExecutorService> replicationQueueExecutor = new InjectedValue<ScheduledExecutorService>();
        private final EmbeddedCacheManagerConfigurationService.TransportConfiguration transport;
        private final InjectedValue<ModuleLoader> moduleLoader = new InjectedValue<ModuleLoader>();

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
        private final InjectedValue<Channel> channel = new InjectedValue<Channel>();
        private final InjectedValue<Executor> executor = new InjectedValue<Executor>();

        private Long lockTimeout;

        void setLockTimeout(long lockTimeout) {
            this.lockTimeout = lockTimeout;
        }

        Injector<Channel> getChannelInjector() {
            return this.channel;
        }

        Injector<Executor> getExecutorInjector() {
            return this.executor;
        }

        @Override
        public Channel getChannel() {
            return this.channel.getValue();
        }

        @Override
        public Executor getExecutor() {
            return this.executor.getOptionalValue();
        }

        @Override
        public Long getLockTimeout() {
            return this.lockTimeout;
        }
    }
}
