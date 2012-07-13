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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;

import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactoryService;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jmx.MBeanServerService;
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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

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
        newControllers.addAll(installRuntimeServices(context, operation, model, verificationHandler));
    }

    Collection<ServiceController<?>> installRuntimeServices(OperationContext context, ModelNode operation, ModelNode containerModel, ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        final PathAddress address = getCacheContainerAddressFromOperation(operation);
        final String name = address.getLastElement().getValue();
        final ServiceTarget target = context.getServiceTarget();

        // pick up the attribute values from the model
        ModelNode resolvedValue = null ;
        // make default cache non required (AS7-3488)
        final String defaultCache = (resolvedValue = CommonAttributes.DEFAULT_CACHE.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final String jndiName = (resolvedValue = CommonAttributes.JNDI_NAME.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
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

        Collection<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>(6);

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

            controllers.add(this.installChannelService(target, name, cluster, stack, verificationHandler));

            for (ChannelDependentServiceProvider provider: ServiceLoader.load(ChannelDependentServiceProvider.class, ChannelDependentServiceProvider.class.getClassLoader())) {
                controllers.add(provider.install(target, name));
            }
        }

        // install the cache container configuration service
        controllers.add(this.installContainerConfigurationService(target, name, defaultCache, moduleId, stack, transportConfig,
                        transportExecutor, listenerExecutor, evictionExecutor, replicationQueueExecutor, verificationHandler));

        // install a cache container service
        controllers.add(this.installContainerService(target, name, aliases, initialMode, verificationHandler));

        // install a name service entry for the cache container
        controllers.add(this.installJndiService(target, name, InfinispanJndiName.createCacheContainerJndiName(jndiName, name), verificationHandler));

        controllers.add(this.installKeyAffinityServiceFactoryService(target, name, verificationHandler));

        log.debugf("%s cache container installed", name);
        return controllers;
     }

     void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = getCacheContainerAddressFromOperation(operation);
        final String containerName = address.getLastElement().getValue();

        // need to remove all container-related services started, in reverse order
        context.removeService(KeyAffinityServiceFactoryService.getServiceName(containerName));

        // remove the BinderService entry
        ModelNode resolvedValue = null;
        final String jndiName = (resolvedValue = CommonAttributes.JNDI_NAME.resolveModelAttribute(context, model)).isDefined() ? resolvedValue.asString() : null;
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(InfinispanJndiName.createCacheContainerJndiName(jndiName, containerName));
        context.removeService(bindInfo.getBinderServiceName());

        // remove the cache container
        context.removeService(EmbeddedCacheManagerService.getServiceName(containerName));
        context.removeService(EmbeddedCacheManagerConfigurationService.getServiceName(containerName));

        // check if a channel was installed
        final ServiceName channelServiceName = ChannelService.getServiceName(containerName) ;
        final ServiceController<?> channelServiceController = context.getServiceRegistry(false).getService(channelServiceName);
        if (channelServiceController != null) {
            for (ChannelDependentServiceProvider provider: ServiceLoader.load(ChannelDependentServiceProvider.class, ChannelDependentServiceProvider.class.getClassLoader())) {
                context.removeService(provider.getServiceName(containerName));
            }
            context.removeService(channelServiceName);
        }
    }

    ServiceController<?> installKeyAffinityServiceFactoryService(ServiceTarget target, String containerName, ServiceVerificationHandler verificationHandler) {
        return AsynchronousService.addService(target, KeyAffinityServiceFactoryService.getServiceName(containerName), new KeyAffinityServiceFactoryService(10), false, true)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    ServiceController<?> installChannelService(ServiceTarget target, String containerName, String cluster, String stack, ServiceVerificationHandler verificationHandler) {

        final InjectedValue<ChannelFactory> channelFactory = new InjectedValue<ChannelFactory>();
        return AsynchronousService.addService(target, ChannelService.getServiceName(containerName), new ChannelService(cluster, channelFactory))
                .addDependency(ChannelFactoryService.getServiceName(stack), ChannelFactory.class, channelFactory)
                .addDependency(EmbeddedCacheManagerService.getServiceName(containerName))
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    PathAddress getCacheContainerAddressFromOperation(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(OP_ADDR)) ;
    }

    ServiceController<?> installContainerConfigurationService(ServiceTarget target,
            String containerName, String defaultCache, ModuleIdentifier moduleId, String stack, Transport transportConfig,
            String transportExecutor, String listenerExecutor, String evictionExecutor, String replicationQueueExecutor,
            ServiceVerificationHandler verificationHandler) {

        final ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(containerName);
        final EmbeddedCacheManagerDependencies dependencies = new EmbeddedCacheManagerDependencies(transportConfig);
        final Service<EmbeddedCacheManagerConfiguration> service = new EmbeddedCacheManagerConfigurationService(containerName, defaultCache, moduleId, dependencies);
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

    ServiceController<?> installContainerService(ServiceTarget target, String containerName, ServiceName[] aliases, ServiceController.Mode initialMode, ServiceVerificationHandler verificationHandler) {

        final ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName);
        final ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(containerName);
        final InjectedValue<EmbeddedCacheManagerConfiguration> config = new InjectedValue<EmbeddedCacheManagerConfiguration>();
        final Service<EmbeddedCacheManager> service = new EmbeddedCacheManagerService(config);
        return target.addService(containerServiceName, service)
                .addDependency(configServiceName, EmbeddedCacheManagerConfiguration.class, config)
                .addAliases(aliases)
                .setInitialMode(initialMode)
                .install()
        ;
    }

    ServiceController<?> installJndiService(ServiceTarget target, String containerName, String jndiName, ServiceVerificationHandler verificationHandler) {

        final ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName);
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);

        final BinderService binder = new BinderService(bindInfo.getBindName());
        return target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName))
                .addDependency(containerServiceName, CacheContainer.class, new ManagedReferenceInjector<CacheContainer>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install()
        ;
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
        private final InjectedValue<ChannelFactory> channelFactory = new InjectedValue<ChannelFactory>();
        private final InjectedValue<Executor> executor = new InjectedValue<Executor>();

        private Long lockTimeout;

        void setLockTimeout(long lockTimeout) {
            this.lockTimeout = lockTimeout;
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
    }
}
