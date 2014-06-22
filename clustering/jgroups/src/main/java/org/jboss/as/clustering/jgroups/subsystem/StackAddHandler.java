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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.ProtocolConfiguration;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.clustering.jgroups.ProtocolStackConfiguration;
import org.jboss.as.clustering.jgroups.RelayConfiguration;
import org.jboss.as.clustering.jgroups.RemoteSiteConfiguration;
import org.jboss.as.clustering.jgroups.TransportConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.threads.JBossExecutors;

/**
 * @author Paul Ferraro
 */
public class StackAddHandler extends AbstractAddStepHandler {

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        // this method is abstract in AbstractAddStepHandler
        // we want to use its more explicit version below, but have to override it anyway
    }

    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        populateModel(operation, resource);
        // add a step to initialize an *optional* TRANSPORT parameter
        if (operation.hasDefined(ModelKeys.TRANSPORT)) {
            // create an ADD operation to add the transport=TRANSPORT child
            ModelNode addTransport = operation.get(ModelKeys.TRANSPORT).clone();

            addTransport.get(OPERATION_NAME).set(ModelDescriptionConstants.ADD);
            ModelNode transportAddress = operation.get(OP_ADDR).clone();
            transportAddress.add(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
            transportAddress.protect();
            addTransport.get(OP_ADDR).set(transportAddress);

            // execute the operation using the transport handler
            context.addStep(addTransport, TransportResourceDefinition.ADD_HANDLER, OperationContext.Stage.MODEL, true);
        }

        // add steps to initialize *optional* PROTOCOL parameters
        if (operation.hasDefined(ModelKeys.PROTOCOLS)) {

            List<ModelNode> protocols = operation.get(ModelKeys.PROTOCOLS).asList();
            // because we use stage IMMEDIATE when creating protocols, unless we reverse the order
            // of the elements in the LIST, they will get applied in reverse order - the last step
            // added gets executed first

            ListIterator<ModelNode> iterator = protocols.listIterator(protocols.size());
            while (iterator.hasPrevious()) {
                ModelNode protocol = iterator.previous();
                // create an ADD operation to add the protocol=* child
                ModelNode addProtocol = protocol.clone();
                addProtocol.get(OPERATION_NAME).set(ModelKeys.ADD_PROTOCOL);
                // add-protocol is a stack operation
                ModelNode protocolAddress = operation.get(OP_ADDR).clone();
                protocolAddress.protect();
                addProtocol.get(OP_ADDR).set(protocolAddress);

                // execute the operation using the transport handler
                context.addStep(addProtocol, ProtocolResourceDefinition.ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        // Because we use child resources in a read-only manner to configure the protocol stack, replace the local model with the full model
        model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        installRuntimeServices(context, operation, model, verificationHandler, newControllers);
    }

    static void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        // check that we have enough information to create a stack
        protocolStackSanityCheck(name, model);

        // we need to preserve the order of the protocols as maintained by PROTOCOLS
        // pick up the ordered protocols here as a List<Property> where property is <name, ModelNode>
        List<Property> orderedProtocols = getOrderedProtocolPropertyList(model);

        // pick up the transport here and its values
        ModelNode transport = model.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
        ModelNode resolvedValue = null;
        final String type = (resolvedValue = TransportResourceDefinition.TYPE.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
        final boolean  shared = TransportResourceDefinition.SHARED.resolveModelAttribute(context, transport).asBoolean();
        final String machine = (resolvedValue = TransportResourceDefinition.MACHINE.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
        final String rack = (resolvedValue = TransportResourceDefinition.RACK.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
        final String site = (resolvedValue = TransportResourceDefinition.SITE.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
        final String timerExecutor = (resolvedValue = TransportResourceDefinition.TIMER_EXECUTOR.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
        final String threadFactory = (resolvedValue = TransportResourceDefinition.THREAD_FACTORY.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
        final String diagnosticsSocketBinding = (resolvedValue = TransportResourceDefinition.DIAGNOSTICS_SOCKET_BINDING.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
        final String defaultExecutor = (resolvedValue = TransportResourceDefinition.DEFAULT_EXECUTOR.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
        final String oobExecutor = (resolvedValue = TransportResourceDefinition.OOB_EXECUTOR.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
        final String transportSocketBinding = (resolvedValue = TransportResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;

        // set up the transport
        Transport transportConfig = new Transport(type);
        transportConfig.setShared(shared);
        transportConfig.setTopology(site, rack, machine);
        initProtocolProperties(context, transport, transportConfig);

        Relay relayConfig = null;
        List<Map.Entry<String, Injector<ChannelFactory>>> stacks = new LinkedList<Map.Entry<String, Injector<ChannelFactory>>>();
        if (model.hasDefined(ModelKeys.RELAY)) {
            final ModelNode relay = model.get(ModelKeys.RELAY, ModelKeys.RELAY_NAME);
            final String siteName = RelayResourceDefinition.SITE.resolveModelAttribute(context, relay).asString();
            relayConfig = new Relay(siteName);
            initProtocolProperties(context, relay, relayConfig);
            if (relay.hasDefined(ModelKeys.REMOTE_SITE)) {
                List<RemoteSiteConfiguration> remoteSites = relayConfig.getRemoteSites();
                for (Property remoteSiteProperty: relay.get(ModelKeys.REMOTE_SITE).asPropertyList()) {
                    final String remoteSiteName = remoteSiteProperty.getName();
                    final ModelNode remoteSite = remoteSiteProperty.getValue();
                    final String cluster = RemoteSiteResourceDefinition.CLUSTER.resolveModelAttribute(context, remoteSite).asString();
                    final String stack = RemoteSiteResourceDefinition.STACK.resolveModelAttribute(context, remoteSite).asString();
                    final InjectedValue<ChannelFactory> channelFactory = new InjectedValue<ChannelFactory>();
                    remoteSites.add(new RemoteSite(remoteSiteName, cluster, channelFactory));
                    stacks.add(new AbstractMap.SimpleImmutableEntry<String, Injector<ChannelFactory>>(stack, channelFactory));
                }
            }
        }

        // set up the protocol stack Protocol objects
        ProtocolStack stackConfig = new ProtocolStack(name, transportConfig, relayConfig);
        List<Map.Entry<Protocol, String>> protocolSocketBindings = new ArrayList<Map.Entry<Protocol, String>>(orderedProtocols.size());
        for (Property protocolProperty : orderedProtocols) {
            ModelNode protocol = protocolProperty.getValue();
            final String protocolType = (resolvedValue = ProtocolResourceDefinition.TYPE.resolveModelAttribute(context, protocol)).isDefined() ? resolvedValue.asString() : null;
            Protocol protocolConfig = new Protocol(protocolType);
            initProtocolProperties(context, protocol, protocolConfig);
            stackConfig.getProtocols().add(protocolConfig);
            final String protocolSocketBinding = (resolvedValue = ProtocolResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, protocol)).isDefined() ? resolvedValue.asString() : null;
            protocolSocketBindings.add(new AbstractMap.SimpleImmutableEntry<Protocol, String>(protocolConfig, protocolSocketBinding));
        }

        // install the default channel factory service
        ServiceController<ChannelFactory> cfsController = installChannelFactoryService(context.getServiceTarget(),
                        name, diagnosticsSocketBinding, defaultExecutor, oobExecutor, timerExecutor, threadFactory,
                        transportSocketBinding, protocolSocketBindings, transportConfig, stackConfig, stacks, verificationHandler);
        if (newControllers != null) {
            newControllers.add(cfsController);
        }
    }

    static void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) {

        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final String name = address.getLastElement().getValue();

        // remove the ChannelFactoryServiceService
        context.removeService(ChannelFactoryService.getServiceName(name));
    }


    private static ServiceController<ChannelFactory> installChannelFactoryService(ServiceTarget target,
                                                                             String name,
                                                                             String diagnosticsSocketBinding,
                                                                             String defaultExecutor,
                                                                             String oobExecutor,
                                                                             String timerExecutor,
                                                                             String threadFactory,
                                                                             String transportSocketBinding,
                                                                             List<Map.Entry<Protocol, String>> protocolSocketBindings,
                                                                             Transport transportConfig,
                                                                             ProtocolStack stackConfig,
                                                                             List<Map.Entry<String, Injector<ChannelFactory>>> stacks,
                                                                             ServiceVerificationHandler verificationHandler) {

        // create the channel factory service builder
        ServiceBuilder<ChannelFactory> builder = target
                .addService(ChannelFactoryService.getServiceName(name), new ChannelFactoryService(stackConfig))
                .addDependency(ProtocolDefaultsService.SERVICE_NAME, ProtocolDefaults.class, stackConfig.getDefaultsInjector())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, stackConfig.getEnvironmentInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
        // add transport dependencies
        addSocketBindingDependency(builder, transportSocketBinding, transportConfig.getSocketBindingInjector());

        for (Map.Entry<Protocol, String> entry: protocolSocketBindings) {
            addSocketBindingDependency(builder, entry.getValue(), entry.getKey().getSocketBindingInjector());
        }

        // add remaining dependencies
        addSocketBindingDependency(builder, diagnosticsSocketBinding, transportConfig.getDiagnosticsSocketBindingInjector());
        addExecutorDependency(builder, defaultExecutor, transportConfig.getDefaultExecutorInjector());
        addExecutorDependency(builder, oobExecutor, transportConfig.getOOBExecutorInjector());
        if (timerExecutor != null) {
            builder.addDependency(ThreadsServices.executorName(timerExecutor), ScheduledExecutorService.class, transportConfig.getTimerExecutorInjector());
        }
        if (threadFactory != null) {
            builder.addDependency(ThreadsServices.threadFactoryName(threadFactory), ThreadFactory.class, transportConfig.getThreadFactoryInjector());
        }
        for (Map.Entry<String, Injector<ChannelFactory>> entry: stacks) {
            builder.addDependency(ChannelFactoryService.getServiceName(entry.getKey()), ChannelFactory.class, entry.getValue());
        }
        return builder.install();
    }

    private static void initProtocolProperties(OperationContext context, ModelNode protocol, Protocol protocolConfig) throws OperationFailedException {

        Map<String, String> properties = protocolConfig.getProperties();
        // properties are a child resource of protocol
        if (protocol.hasDefined(ModelKeys.PROPERTY)) {
            for (Property property : protocol.get(ModelKeys.PROPERTY).asPropertyList()) {
                // the format of the property elements
                //  "property" => {
                //       "relative-to" => {"value" => "fred"},
                //   }
                String propertyName = property.getName();
                // get the value from the ModelNode {"value" => "fred"}
                ModelNode propertyValue = null;
                propertyValue = PropertyResourceDefinition.VALUE.resolveModelAttribute(context, property.getValue());

                properties.put(propertyName, propertyValue.asString());
            }
       }
    }

    public static List<Property> getOrderedProtocolPropertyList(ModelNode stack) {
        ModelNode orderedProtocols = new ModelNode();

        // check for the empty ordering list
        if  (!stack.hasDefined(ModelKeys.PROTOCOLS)) {
            return null;
        }
        // PROTOCOLS is a list of protocol names only, reflecting the order in which protocols were added to the stack
        List<ModelNode> protocolOrdering = stack.get(ModelKeys.PROTOCOLS).clone().asList();

        // now construct an ordered list of the full protocol model nodes
        ModelNode unorderedProtocols = stack.get(ModelKeys.PROTOCOL);
        for (ModelNode protocolName : protocolOrdering) {
            ModelNode protocolModel = unorderedProtocols.get(protocolName.asString());
            orderedProtocols.add(protocolName.asString(), protocolModel);
        }
        return orderedProtocols.asPropertyList();
    }

    private static void addSocketBindingDependency(ServiceBuilder<ChannelFactory> builder, String socketBinding, Injector<SocketBinding> injector) {
        if (socketBinding != null) {
            builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(socketBinding), SocketBinding.class, injector);
        }
    }

    private static void addExecutorDependency(ServiceBuilder<ChannelFactory> builder, String executor, Injector<Executor> injector) {
        if (executor != null) {
            builder.addDependency(ThreadsServices.executorName(executor), Executor.class, injector);
        }
    }

    /*
     * A check that we have the minimal configuration required to create a protocol stack.
     */
    private static void protocolStackSanityCheck(String stackName, ModelNode model) throws OperationFailedException {

         ModelNode transport = model.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
         if (!transport.isDefined()) {
            throw JGroupsLogger.ROOT_LOGGER.transportNotDefined(stackName);
         }

         List<Property> protocols = getOrderedProtocolPropertyList(model);
         if ( protocols == null || !(protocols.size() > 0)) {
             throw JGroupsLogger.ROOT_LOGGER.protocolListNotDefined(stackName);
         }
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    static class ProtocolStack implements ProtocolStackConfiguration {
        private final InjectedValue<ProtocolDefaults> defaults = new InjectedValue<ProtocolDefaults>();
        private final InjectedValue<ServerEnvironment> environment = new InjectedValue<ServerEnvironment>();

        private final String name;
        private final TransportConfiguration transport;
        private final RelayConfiguration relay;
        private final List<ProtocolConfiguration> protocols = new LinkedList<ProtocolConfiguration>();

        ProtocolStack(String name, TransportConfiguration transport, RelayConfiguration relay) {
            this.name = name;
            this.transport = transport;
            this.relay = relay;
        }

        Injector<ProtocolDefaults> getDefaultsInjector() {
            return this.defaults;
        }

        Injector<ServerEnvironment> getEnvironmentInjector() {
            return this.environment;
        }

        @Override
        public TransportConfiguration getTransport() {
            return this.transport;
        }

        @Override
        public List<ProtocolConfiguration> getProtocols() {
            return this.protocols;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public ProtocolDefaults getDefaults() {
            return this.defaults.getValue();
        }

        @Override
        public ServerEnvironment getEnvironment() {
            return this.environment.getValue();
        }

        @Override
        public RelayConfiguration getRelay() {
            return this.relay;
        }
    }

    static class Transport extends Protocol implements TransportConfiguration {
        private final InjectedValue<SocketBinding> diagnosticsSocketBinding = new InjectedValue<SocketBinding>();
        private final InjectedValue<Executor> defaultExecutor = new InjectedValue<Executor>();
        private final InjectedValue<Executor> oobExecutor = new InjectedValue<Executor>();
        private final InjectedValue<ScheduledExecutorService> timerExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ThreadFactory> threadFactory = new InjectedValue<ThreadFactory>();
        private boolean shared = true;
        private Topology topology;

        Transport(String name) {
            super(name);
        }

        Injector<SocketBinding> getDiagnosticsSocketBindingInjector() {
            return this.diagnosticsSocketBinding;
        }

        Injector<Executor> getDefaultExecutorInjector() {
            return this.defaultExecutor;
        }

        Injector<Executor> getOOBExecutorInjector() {
            return this.oobExecutor;
        }

        Injector<ScheduledExecutorService> getTimerExecutorInjector() {
            return this.timerExecutor;
        }

        Injector<ThreadFactory> getThreadFactoryInjector() {
            return this.threadFactory;
        }

        void setShared(boolean shared) {
            this.shared = shared;
        }

        @Override
        public boolean isShared() {
            return this.shared;
        }

        @Override
        public Topology getTopology() {
            return this.topology;
        }

        public void setTopology(String site, String rack, String machine) {
            if ((site != null) || (rack != null) || (machine != null)) {
                this.topology = new TopologyImpl(site, rack, machine);
            }
        }

        @Override
        public SocketBinding getDiagnosticsSocketBinding() {
            return this.diagnosticsSocketBinding.getOptionalValue();
        }

        @Override
        public ExecutorService getDefaultExecutor() {
            Executor executor = this.defaultExecutor.getOptionalValue();
            return (executor != null) ? JBossExecutors.protectedExecutorService(executor) : null;
        }

        @Override
        public ExecutorService getOOBExecutor() {
            Executor executor = this.oobExecutor.getOptionalValue();
            return (executor != null) ? JBossExecutors.protectedExecutorService(executor) : null;
        }

        @Override
        public ScheduledExecutorService getTimerExecutor() {
            return this.timerExecutor.getOptionalValue();
        }

        @Override
        public ThreadFactory getThreadFactory() {
            return this.threadFactory.getOptionalValue();
        }

        private class TopologyImpl implements Topology {
            private final String site;
            private final String rack;
            private final String machine;

            TopologyImpl(String site, String rack, String machine) {
                this.site = site;
                this.rack = rack;
                this.machine = machine;
            }

            @Override
            public String getMachine() {
                return this.machine;
            }

            @Override
            public String getRack() {
                return this.rack;
            }

            @Override
            public String getSite() {
                return this.site;
            }
        }
    }

    static class Relay extends Protocol implements RelayConfiguration {
        private final List<RemoteSiteConfiguration> remoteSites = new LinkedList<RemoteSiteConfiguration>();
        private final String siteName;

        Relay(String siteName) {
            super("relay.RELAY2");
            this.siteName = siteName;
        }

        @Override
        public String getSiteName() {
            return this.siteName;
        }

        @Override
        public List<RemoteSiteConfiguration> getRemoteSites() {
            return this.remoteSites;
        }
    }

    static class RemoteSite implements RemoteSiteConfiguration {
        private final Value<ChannelFactory> channelFactory;
        private final String name;
        private final String clusterName;

        RemoteSite(String name, String clusterName, Value<ChannelFactory> channelFactory) {
            this.name = name;
            this.clusterName = clusterName;
            this.channelFactory = channelFactory;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public ChannelFactory getChannelFactory() {
            return this.channelFactory.getValue();
        }

        @Override
        public String getClusterName() {
            return this.clusterName;
        }
    }

    static class Protocol implements ProtocolConfiguration {
        private final String name;
        private final InjectedValue<SocketBinding> socketBinding = new InjectedValue<SocketBinding>();
        private final Map<String, String> properties = new HashMap<String, String>();
        final Class<?> protocolClass;

        Protocol(final String name) {
            this.name = name;
            PrivilegedAction<Class<?>> action = new PrivilegedAction<Class<?>>() {
                @Override
                public Class<?> run() {
                    try {
                        return Protocol.this.getClass().getClassLoader().loadClass(org.jgroups.conf.ProtocolConfiguration.protocol_prefix + '.' + name);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
            this.protocolClass = AccessController.doPrivileged(action);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public boolean hasProperty(final String property) {
            PrivilegedAction<Field> action = new PrivilegedAction<Field>() {
                @Override
                public Field run() {
                    return getField(Protocol.this.protocolClass, property);
                }
            };
            return AccessController.doPrivileged(action) != null;
        }

        static Field getField(Class<?> targetClass, String property) {
            try {
                return targetClass.getDeclaredField(property);
            } catch (NoSuchFieldException e) {
                Class<?> superClass = targetClass.getSuperclass();
                return (superClass != null) && org.jgroups.stack.Protocol.class.isAssignableFrom(superClass) ? getField(superClass, property) : null;
            }
        }

        @Override
        public Map<String, String> getProperties() {
            return this.properties;
        }

        Injector<SocketBinding> getSocketBindingInjector() {
            return this.socketBinding;
        }

        @Override
        public SocketBinding getSocketBinding() {
            return this.socketBinding.getOptionalValue();
        }
    }
}
