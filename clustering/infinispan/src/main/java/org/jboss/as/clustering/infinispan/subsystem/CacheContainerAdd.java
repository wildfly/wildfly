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
import org.jboss.msc.value.Value;
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
        target.get(ModelKeys.DEFAULT_CACHE).set(source.require(ModelKeys.DEFAULT_CACHE));
        if (source.hasDefined(ModelKeys.JNDI_NAME)) {
            target.get(ModelKeys.JNDI_NAME).set(source.get(ModelKeys.JNDI_NAME));
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
        if (source.hasDefined(ModelKeys.ALIAS)) {
            ModelNode aliases = target.get(ModelKeys.ALIAS);
            for (ModelNode alias : source.get(ModelKeys.ALIAS).asList()) {
                aliases.add(alias);
            }
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

        String defaultCache = model.require(ModelKeys.DEFAULT_CACHE).asString();

        Transport transportConfig = new Transport();
        EmbeddedCacheManagerDependencies dependencies = new EmbeddedCacheManagerDependencies(transportConfig);

        ServiceName[] aliases = null;
        if (model.hasDefined(ModelKeys.ALIAS)) {
            List<ModelNode> list = operation.get(ModelKeys.ALIAS).asList();
            aliases = new ServiceName[list.size()];
            for (int i = 0; i < list.size(); i++) {
                aliases[i] = EmbeddedCacheManagerService.getServiceName(list.get(i).asString());
            }
        }

        ServiceTarget target = context.getServiceTarget();
        ServiceName serviceName = EmbeddedCacheManagerService.getServiceName(name);
        ServiceBuilder<EmbeddedCacheManager> containerBuilder = target.addService(serviceName, new EmbeddedCacheManagerService(name, defaultCache, dependencies))
                .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, dependencies.getMBeanServerInjector())
                .addAliases(aliases)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        String jndiName = (model.hasDefined(ModelKeys.JNDI_NAME) ? InfinispanJndiName.toJndiName(model.get(ModelKeys.JNDI_NAME).asString()) : InfinispanJndiName.defaultCacheContainerJndiName(name)).getAbsoluteName();
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<ManagedReferenceFactory> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName))
                .addDependency(serviceName, CacheContainer.class, new ManagedReferenceInjector<CacheContainer>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
        newControllers.add(binderBuilder.install());

        // Because caches are added after this method completes, we can never be sure whether or not
        // we need a transport. The service TransportRequiredService is set by ClusteredCacheAdd subclasses
        // to indicate that a transport needs to be initialised in the enclosing cache container.
        // The transport will only be initialised in EmbeddedCacheManagerService.start() if this value is true.
        // Here, we always assume that a transport may be required and so perform the necessary setup.
        String stack = null;

        //
        // the transport is stored as a child resource /subsystem=infinispan/cache-container=name/singleton=transport
        // in the form of a ModelNode, so we need to retrieve it from the model
        //
        if (model.hasDefined(ModelKeys.SINGLETON)) {
            ModelNode transport = model.get(ModelKeys.SINGLETON, ModelKeys.TRANSPORT);
            if (transport.hasDefined(ModelKeys.STACK)) {
                stack = transport.get(ModelKeys.STACK).asString();
            }
            if (transport.hasDefined(ModelKeys.LOCK_TIMEOUT)) {
                transportConfig.setLockTimeout(transport.get(ModelKeys.LOCK_TIMEOUT).asLong());
            }
            if (transport.hasDefined(ModelKeys.SITE)) {
                transportConfig.setSite(transport.get(ModelKeys.SITE).asString());
            }
            if (transport.hasDefined(ModelKeys.RACK)) {
                transportConfig.setRack(transport.get(ModelKeys.RACK).asString());
            }
            if (transport.hasDefined(ModelKeys.MACHINE)) {
                transportConfig.setMachine(transport.get(ModelKeys.MACHINE).asString());
            }
            addExecutorDependency(containerBuilder, transport, ModelKeys.EXECUTOR, transportConfig.getExecutorInjector());
        }

        ServiceName channelServiceName = ChannelService.getServiceName(name);
        // add an optional dependency on a ChannelService which has the cache-container name
        containerBuilder.addDependency(DependencyType.OPTIONAL, channelServiceName, Channel.class, transportConfig.getChannelInjector());

        addExecutorDependency(containerBuilder, model, ModelKeys.LISTENER_EXECUTOR, dependencies.getListenerExecutorInjector());
        addScheduledExecutorDependency(containerBuilder, model, ModelKeys.EVICTION_EXECUTOR, dependencies.getEvictionExecutorInjector());
        addScheduledExecutorDependency(containerBuilder, model, ModelKeys.REPLICATION_QUEUE_EXECUTOR, dependencies.getReplicationQueueExecutorInjector());

        newControllers.add(containerBuilder.install());

        InjectedValue<ChannelFactory> channelFactory = new InjectedValue<ChannelFactory>();
        // Set initial mode to NEVER - Cache add operations will update mode to ON_DEMAND when necessary
        ServiceBuilder<Channel> channelBuilder = target.addService(channelServiceName, new ChannelService(name, channelFactory))
                .addAliases(EmbeddedCacheManagerService.getTransportServiceName(name))
                .addDependency(ChannelFactoryService.getServiceName(stack), ChannelFactory.class, channelFactory)
                .setInitialMode(ServiceController.Mode.NEVER)
        ;
        newControllers.add(channelBuilder.install());

        log.debugf("Cache container %s installed", name);
     }

    private void addExecutorDependency(ServiceBuilder<EmbeddedCacheManager> builder, ModelNode model, String key, Injector<Executor> injector) {
        if (model.hasDefined(key)) {
            builder.addDependency(ThreadsServices.executorName(model.get(key).asString()), Executor.class, injector);
        }
    }

    private void addScheduledExecutorDependency(ServiceBuilder<EmbeddedCacheManager> builder, ModelNode model, String key, Injector<ScheduledExecutorService> injector) {
        if (model.hasDefined(key)) {
            builder.addDependency(ThreadsServices.executorName(model.get(key).asString()), ScheduledExecutorService.class, injector);
        }
    }

    static class EmbeddedCacheManagerDependencies implements EmbeddedCacheManagerService.Dependencies {
        private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
        private final InjectedValue<Executor> listenerExecutor = new InjectedValue<Executor>();
        private final InjectedValue<ScheduledExecutorService> evictionExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ScheduledExecutorService> replicationQueueExecutor = new InjectedValue<ScheduledExecutorService>();

        private final EmbeddedCacheManagerService.TransportConfiguration transport;

        EmbeddedCacheManagerDependencies(EmbeddedCacheManagerService.TransportConfiguration transport) {
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

        @Override
        public EmbeddedCacheManagerService.TransportConfiguration getTransportConfiguration() {
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
    }

    static class Transport implements EmbeddedCacheManagerService.TransportConfiguration {
        private final InjectedValue<Channel> channel = new InjectedValue<Channel>();
        private final InjectedValue<Executor> executor = new InjectedValue<Executor>();

        private Long lockTimeout;
        private String site;
        private String rack;
        private String machine;

        void setLockTimeout(long lockTimeout) {
            this.lockTimeout = lockTimeout;
        }

        void setSite(String site) {
            this.site = site;
        }

        void setRack(String rack) {
            this.rack = rack;
        }

        void setMachine(String machine) {
            this.machine = machine;
        }

        Injector<Channel> getChannelInjector() {
            return this.channel;
        }

        Injector<Executor> getExecutorInjector() {
            return this.executor;
        }

        @Override
        public Value<Channel> getChannel() {
            return this.channel;
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
        public String getSite() {
            return this.site;
        }

        @Override
        public String getRack() {
            return this.rack;
        }

        @Override
        public String getMachine() {
            return this.machine;
        }
    }
}
