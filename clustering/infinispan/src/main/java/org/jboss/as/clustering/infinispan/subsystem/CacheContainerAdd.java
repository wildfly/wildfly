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

import javax.management.MBeanServer;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.infinispan.config.Configuration;
import org.infinispan.manager.CacheContainer;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.txn.service.TxnServices;
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
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.jgroups.Channel;

/**
 * @author Paul Ferraro
 * @author Tristan Tarrant
 * @author Richard Achmatowicz
 */
public class CacheContainerAdd extends AbstractAddStepHandler implements DescriptionProvider {

    private static final Logger log = Logger.getLogger(CacheContainerAdd.class.getPackage().getName());

    public static final CacheContainerAdd INSTANCE = new CacheContainerAdd();

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        populate(existing, operation);
        return operation;
    }

    static String getContainerJNDIName(ModelNode container, String name) {
        JndiName jndiName = null ;
        if (container.hasDefined(ModelKeys.JNDI_NAME)) {
            // convert the JNDI name passed by the user
            jndiName = toJndiName(container.get(ModelKeys.JNDI_NAME).asString()) ;
        }
        else {
            // build the name from scratch as java:jboss/infinispan/<container name>
            jndiName = JndiName.of("java:jboss").append(InfinispanExtension.SUBSYSTEM_NAME).append(name) ;
        }
        return jndiName.getAbsoluteName();
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
        if (source.hasDefined(ModelKeys.TRANSPORT)) {
            target.get(ModelKeys.TRANSPORT).set(source.get(ModelKeys.TRANSPORT));
        }
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        populate(operation, model);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        String defaultCache = operation.require(ModelKeys.DEFAULT_CACHE).asString();

        EmbeddedCacheManager config = new EmbeddedCacheManager(name, defaultCache);

        ServiceName[] aliases = null;
        if (operation.hasDefined(ModelKeys.ALIAS)) {
            List<ModelNode> list = operation.get(ModelKeys.ALIAS).asList();
            aliases = new ServiceName[list.size()];
            for (int i = 0; i < list.size(); i++) {
                aliases[i] = EmbeddedCacheManagerService.getServiceName(list.get(i).asString());
            }
        }

        ServiceTarget target = context.getServiceTarget();
        ServiceName serviceName = EmbeddedCacheManagerService.getServiceName(name);
        ServiceBuilder<CacheContainer> builder = target.addService(serviceName, new EmbeddedCacheManagerService(config))
                .addDependency(EmbeddedCacheManagerDefaultsService.SERVICE_NAME, EmbeddedCacheManagerDefaults.class, config.getDefaultsInjector())
                .addDependency(DependencyType.OPTIONAL, TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, config.getTransactionManagerInjector())
                .addDependency(DependencyType.OPTIONAL, TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, config.getTransactionSynchronizationRegistryInjector())
                .addDependency(DependencyType.OPTIONAL, TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class, config.getXAResourceRecoveryRegistryInjector())
                .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, config.getMBeanServerInjector())
                .addAliases(aliases)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);

        String jndiName = (operation.hasDefined(ModelKeys.JNDI_NAME) ? toJndiName(operation.get(ModelKeys.JNDI_NAME).asString()) : JndiName.of("java:jboss").append(InfinispanExtension.SUBSYSTEM_NAME).append(name)).getAbsoluteName();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);

        BinderService binder = new BinderService(bindInfo.getBindName());
        newControllers.add(target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName))
                .addDependency(serviceName, CacheContainer.class, new ManagedReferenceInjector<CacheContainer>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install());

        // Because caches are added after this method completes, we can never be sure whether or not
        // we need a transport. The service TransportRequiredService is set by ClusteredCacheAdd subclasses
        // to indicate that a transport needs to be initialised in the enclosing cache container.
        // The transport will only be initialised in EmbeddedCacheManagerService.start() if this value is true.
        // Here, we always assume that a transport may be required and so perform the necessary setup.
        Transport transportConfig = new Transport();
        String stack = null;
        if (operation.hasDefined(ModelKeys.TRANSPORT)) {
            ModelNode transport = operation.get(ModelKeys.TRANSPORT);
            if (transport.hasDefined(ModelKeys.STACK)) {
                stack = transport.get(ModelKeys.STACK).asString();
            }
            addExecutorDependency(builder, transport, ModelKeys.EXECUTOR, transportConfig.getExecutorInjector());
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
        }

        // add a service to identify whether clustered caching is required
        TransportRequiredService transportRequired = new TransportRequiredService(new AtomicBoolean(false));
        ServiceName transportRequiredServiceName = TransportRequiredService.getServiceName(name);
        log.debugf("Adding TransportRequired service: %s", transportRequiredServiceName.toString());

        ServiceBuilder transportRequiredBuilder = target.addService(transportRequiredServiceName, transportRequired);
        transportRequiredBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        newControllers.add(transportRequiredBuilder.install());

        // add a dependency on a service to indicate if transport is required
        // the CacheManagerService will therefore pick up the reference from its config
        builder.addDependency(transportRequiredServiceName, AtomicBoolean.class, config.getTransportRequiredInjector());

        // add an optional dependency on a ChannelService which has the cache-container name
        builder.addDependency(DependencyType.OPTIONAL, ChannelService.getServiceName(name), Channel.class, transportConfig.getChannelInjector());
        config.setTransport(transportConfig);

        // add the channel service and set up the appropriate dependencies
        // but only if the required ChannelFactoryService is installed (i.e. in the ServiceRegistry)
        if (context.getServiceRegistry(false).getService(ChannelFactoryService.getServiceName(stack)) != null) {
            InjectedValue<ChannelFactory> channelFactory = new InjectedValue<ChannelFactory>();
            newControllers.add(target.addService(ChannelService.getServiceName(name), new ChannelService(name, channelFactory))
                    .addDependency(ChannelFactoryService.getServiceName(stack), ChannelFactory.class, channelFactory)
                    .setInitialMode(ServiceController.Mode.ON_DEMAND)
                    .install());
        }
        addExecutorDependency(builder, operation, ModelKeys.LISTENER_EXECUTOR, config.getListenerExecutorInjector());
        addScheduledExecutorDependency(builder, operation, ModelKeys.EVICTION_EXECUTOR, config.getEvictionExecutorInjector());
        addScheduledExecutorDependency(builder, operation, ModelKeys.REPLICATION_QUEUE_EXECUTOR, config.getReplicationQueueExecutorInjector());

        newControllers.add(builder.install());
        log.debug("cache container " + name + " installed");
     }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getCacheContainerAddDescription(locale);
    }

    private void addExecutorDependency(ServiceBuilder<CacheContainer> builder, ModelNode model, String key, Injector<Executor> injector) {
        if (model.hasDefined(key)) {
            builder.addDependency(ThreadsServices.executorName(model.get(key).asString()), Executor.class, injector);
        }
    }

    private void addScheduledExecutorDependency(ServiceBuilder<CacheContainer> builder, ModelNode model, String key, Injector<ScheduledExecutorService> injector) {
        if (model.hasDefined(key)) {
            builder.addDependency(ThreadsServices.executorName(model.get(key).asString()), ScheduledExecutorService.class, injector);
        }
    }

    private static JndiName toJndiName(String value) {
        return value.startsWith("java:") ? JndiName.of(value) : JndiName.of("java:jboss").append(value.startsWith("/") ? value.substring(1) : value);
    }

    static class EmbeddedCacheManager implements EmbeddedCacheManagerConfiguration {
        private final InjectedValue<EmbeddedCacheManagerDefaults> defaults = new InjectedValue<EmbeddedCacheManagerDefaults>();
        private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();
        private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistry = new InjectedValue<TransactionSynchronizationRegistry>();
        private final InjectedValue<XAResourceRecoveryRegistry> recoveryRegistry = new InjectedValue<XAResourceRecoveryRegistry>();
        private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
        private final InjectedValue<Executor> listenerExecutor = new InjectedValue<Executor>();
        private final InjectedValue<ScheduledExecutorService> evictionExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ScheduledExecutorService> replicationQueueExecutor = new InjectedValue<ScheduledExecutorService>();
        // indicates if we should install a transport
        private final InjectedValue<AtomicBoolean> transportRequired = new InjectedValue<AtomicBoolean>();

        private final String name;
        private final String defaultCache;
        private final Map<String, Configuration> configurations = new HashMap<String, Configuration>();
        private Transport transport;

        EmbeddedCacheManager(String name, String defaultCache) {
            this.name = name;
            this.defaultCache = defaultCache;
        }

        Injector<EmbeddedCacheManagerDefaults> getDefaultsInjector() {
            return this.defaults;
        }

        Injector<TransactionManager> getTransactionManagerInjector() {
            return this.transactionManager;
        }

        Injector<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistryInjector() {
            return this.transactionSynchronizationRegistry;
        }

        Injector<XAResourceRecoveryRegistry> getXAResourceRecoveryRegistryInjector() {
            return this.recoveryRegistry;
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

        Injector<AtomicBoolean> getTransportRequiredInjector() {
            return this.transportRequired;
        }

        void setTransport(Transport transport) {
            this.transport = transport;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getDefaultCache() {
            return this.defaultCache;
        }

        @Override
        public Map<String, Configuration> getConfigurations() {
            return this.configurations;
        }

        @Override
        public TransportConfiguration getTransportConfiguration() {
            return this.transport;
        }

        @Override
        public EmbeddedCacheManagerDefaults getDefaults() {
            return this.defaults.getValue();
        }

        @Override
        public Value<TransactionManager> getTransactionManager() {
            return this.transactionManager;
        }

        @Override
        public Value<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistry() {
            return this.transactionSynchronizationRegistry;
        }

        @Override
        public XAResourceRecoveryRegistry getXAResourceRecoveryRegistry() {
            return this.recoveryRegistry.getOptionalValue();
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
        public AtomicBoolean getTransportRequired() {
            return this.transportRequired.getValue();
        }
    }

    static class Transport implements TransportConfiguration {
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
