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
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
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

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) {
        // AS7-3488 make default-cache non required attribute
        // target.get(ModelKeys.DEFAULT_CACHE).set(source.get(ModelKeys.DEFAULT_CACHE));
        if (source.hasDefined(ModelKeys.DEFAULT_CACHE)) {
            target.get(ModelKeys.DEFAULT_CACHE).set(source.get(ModelKeys.DEFAULT_CACHE));
        }
        if (source.hasDefined(ModelKeys.ALIASES)) {
            target.get(ModelKeys.ALIASES).set(source.get(ModelKeys.ALIASES));
        }
        if (source.hasDefined(ModelKeys.JNDI_NAME)) {
            target.get(ModelKeys.JNDI_NAME).set(source.get(ModelKeys.JNDI_NAME));
        }
        if (source.hasDefined(ModelKeys.START)) {
            target.get(ModelKeys.START).set(source.get(ModelKeys.START));
        }
        if (source.hasDefined(ModelKeys.LISTENER_EXECUTOR)) {
            target.get(ModelKeys.LISTENER_EXECUTOR).set(source.get(ModelKeys.LISTENER_EXECUTOR));
        }
        if (source.hasDefined(ModelKeys.EVICTION_EXECUTOR)) {
            target.get(ModelKeys.EVICTION_EXECUTOR).set(source.get(ModelKeys.EVICTION_EXECUTOR));
        }
        if (source.hasDefined(ModelKeys.REPLICATION_QUEUE_EXECUTOR)) {
            target.get(ModelKeys.REPLICATION_QUEUE_EXECUTOR).set(source.get(ModelKeys.REPLICATION_QUEUE_EXECUTOR));
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        populate(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        // Because we use child resources in a read-only manner to configure the cache container, replace the local model with the full model
        model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        // make default cache non required (AS7-3488)
        String defaultCache = model.get(ModelKeys.DEFAULT_CACHE).asString();

        boolean hasTransport = model.hasDefined(ModelKeys.TRANSPORT) && model.get(ModelKeys.TRANSPORT).hasDefined(ModelKeys.TRANSPORT_NAME);
        Transport transportConfig = hasTransport ? new Transport() : null;
        EmbeddedCacheManagerDependencies dependencies = new EmbeddedCacheManagerDependencies(transportConfig);

        ServiceName[] aliases = null;
        if (model.hasDefined(ModelKeys.ALIASES)) {
            List<ModelNode> list = operation.get(ModelKeys.ALIASES).asList();
            aliases = new ServiceName[list.size()];
            for (int i = 0; i < list.size(); i++) {
                aliases[i] = EmbeddedCacheManagerService.getServiceName(list.get(i).asString());
            }
        }

        ServiceTarget target = context.getServiceTarget();
        ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(name);
        ServiceBuilder<EmbeddedCacheManagerConfiguration> configBuilder = target.addService(configServiceName, new EmbeddedCacheManagerConfigurationService(name, defaultCache, dependencies))
                .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, dependencies.getMBeanServerInjector())
                .addDependency(DependencyType.OPTIONAL, EmbeddedCacheManagerConfigurationService.getClassLoaderServiceName(name), ClassLoader.class, dependencies.getClassLoaderInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        ServiceController.Mode initialMode = model.hasDefined(ModelKeys.START) ? StartMode.valueOf(model.get(ModelKeys.START).asString()).getMode() : ServiceController.Mode.ON_DEMAND;

        ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(name);
        InjectedValue<EmbeddedCacheManagerConfiguration> config = new InjectedValue<EmbeddedCacheManagerConfiguration>();
        ServiceBuilder<EmbeddedCacheManager> containerBuilder = target.addService(containerServiceName, new EmbeddedCacheManagerService(config))
                .addDependency(configServiceName, EmbeddedCacheManagerConfiguration.class, config)
                .addAliases(aliases)
                .setInitialMode(initialMode)
        ;
        newControllers.add(containerBuilder.install());

        String jndiName = (model.hasDefined(ModelKeys.JNDI_NAME) ? InfinispanJndiName.toJndiName(model.get(ModelKeys.JNDI_NAME).asString()) : InfinispanJndiName.defaultCacheContainerJndiName(name)).getAbsoluteName();
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<ManagedReferenceFactory> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName))
                .addDependency(containerServiceName, CacheContainer.class, new ManagedReferenceInjector<CacheContainer>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;
        newControllers.add(binderBuilder.install());

        if (hasTransport) {
            String stack = null;
            ModelNode transport = model.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
            if (transport.hasDefined(ModelKeys.STACK)) {
                stack = transport.get(ModelKeys.STACK).asString();
            }
            if (transport.hasDefined(ModelKeys.LOCK_TIMEOUT)) {
                transportConfig.setLockTimeout(transport.get(ModelKeys.LOCK_TIMEOUT).asLong());
            }
            addExecutorDependency(configBuilder, transport, ModelKeys.EXECUTOR, transportConfig.getExecutorInjector());

            ServiceName channelServiceName = ChannelService.getServiceName(name);
            configBuilder.addDependency(channelServiceName, Channel.class, transportConfig.getChannelInjector());

            InjectedValue<ChannelFactory> channelFactory = new InjectedValue<ChannelFactory>();
            ServiceBuilder<Channel> channelBuilder = target.addService(channelServiceName, new ChannelService(name, channelFactory))
                    .addDependency(ChannelFactoryService.getServiceName(stack), ChannelFactory.class, channelFactory)
                    .setInitialMode(ServiceController.Mode.ON_DEMAND)
            ;
            newControllers.add(channelBuilder.install());
        }

        addExecutorDependency(configBuilder, model, ModelKeys.LISTENER_EXECUTOR, dependencies.getListenerExecutorInjector());
        addScheduledExecutorDependency(configBuilder, model, ModelKeys.EVICTION_EXECUTOR, dependencies.getEvictionExecutorInjector());
        addScheduledExecutorDependency(configBuilder, model, ModelKeys.REPLICATION_QUEUE_EXECUTOR, dependencies.getReplicationQueueExecutorInjector());

        newControllers.add(configBuilder.install());

        log.debugf("%s cache container installed", name);
     }

    private void addExecutorDependency(ServiceBuilder<EmbeddedCacheManagerConfiguration> builder, ModelNode model, String key, Injector<Executor> injector) {
        if (model.hasDefined(key)) {
            builder.addDependency(ThreadsServices.executorName(model.get(key).asString()), Executor.class, injector);
        }
    }

    private void addScheduledExecutorDependency(ServiceBuilder<EmbeddedCacheManagerConfiguration> builder, ModelNode model, String key, Injector<ScheduledExecutorService> injector) {
        if (model.hasDefined(key)) {
            builder.addDependency(ThreadsServices.executorName(model.get(key).asString()), ScheduledExecutorService.class, injector);
        }
    }

    static class EmbeddedCacheManagerDependencies implements EmbeddedCacheManagerConfigurationService.Dependencies {
        private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
        private final InjectedValue<Executor> listenerExecutor = new InjectedValue<Executor>();
        private final InjectedValue<ScheduledExecutorService> evictionExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ScheduledExecutorService> replicationQueueExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ClassLoader> loader = new InjectedValue<ClassLoader>();
        private final EmbeddedCacheManagerConfigurationService.TransportConfiguration transport;

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

        Injector<ClassLoader> getClassLoaderInjector() {
            return this.loader;
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
        public ClassLoader getClassLoader() {
            return this.loader.getOptionalValue();
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
