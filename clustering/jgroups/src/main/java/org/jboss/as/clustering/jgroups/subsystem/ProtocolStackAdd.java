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

import javax.management.MBeanServer;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.JGroupsMessages;
import org.jboss.as.clustering.jgroups.ProtocolConfiguration;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.clustering.jgroups.ProtocolStackConfiguration;
import org.jboss.as.clustering.jgroups.TransportConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossExecutors;

/**
 * @author Paul Ferraro
 */
public class ProtocolStackAdd extends AbstractAddStepHandler {

    public static final ProtocolStackAdd INSTANCE = new ProtocolStackAdd();

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) {
        // nothing to do for a basic add
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) {
        // this method is abstract in AbstractAddStepHandler
        // we want to use its more explicit version below, but have to override it anyway
    }

    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) {
        final ModelNode model = resource.getModel();

        // handle the basic add() operation parameters
        populate(operation, model);

        // add a step to initialize an *optional* TRANSPORT parameter
        if (operation.hasDefined(ModelKeys.TRANSPORT)) {
            // create an ADD operation to add the transport=TRANSPORT child
            ModelNode addTransport = operation.get(ModelKeys.TRANSPORT).clone() ;

            addTransport.get(OPERATION_NAME).set(ModelDescriptionConstants.ADD);
            ModelNode transportAddress = operation.get(OP_ADDR).clone();
            transportAddress.add(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
            transportAddress.protect();
            addTransport.get(OP_ADDR).set(transportAddress);

            // execute the operation using the transport handler
            context.addStep(addTransport, StackConfigOperationHandlers.TRANSPORT_ADD, OperationContext.Stage.IMMEDIATE);
        }

        // add steps to initialize *optional* PROTOCOL parameters
        if (operation.hasDefined(ModelKeys.PROTOCOLS)) {

            ModelNode protocolsNode = operation.get(ModelKeys.PROTOCOLS);
            List<ModelNode> protocols = operation.get(ModelKeys.PROTOCOLS).asList();
            // because we use stage IMMEDIATE when creating protocols, unless we reverse the order
            // of the elements in the LIST, they will get applied in reverse order - the last step
            // added gets executed first

            for (int i = protocols.size()-1; i >= 0; i--) {
                ModelNode protocol = protocols.get(i);

                // create an ADD operation to add the protocol=* child
                ModelNode addProtocol = protocol.clone() ;
                addProtocol.get(OPERATION_NAME).set(ModelKeys.ADD_PROTOCOL);
                // add-protocol is a stack operation
                ModelNode protocolAddress = operation.get(OP_ADDR).clone();
                protocolAddress.protect();
                addProtocol.get(OP_ADDR).set(protocolAddress);

                // execute the operation using the transport handler
                context.addStep(addProtocol, StackConfigOperationHandlers.PROTOCOL_ADD, OperationContext.Stage.IMMEDIATE);
            }
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        // Because we use child resources in a read-only manner to configure the protocol stack, replace the local model with the full model
        model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        // check that we have enough information to create a stack
        protocolStackSanityCheck(name, model);

        // pick up the transport here and its values
        ModelNode transport = model.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);

        ModelNode resolvedValue = null ;
        final String type = ((resolvedValue = CommonAttributes.TYPE.resolveModelAttribute(context, transport)).isDefined()) ? resolvedValue.asString() : null ;
        final boolean  shared = CommonAttributes.SHARED.resolveModelAttribute(context, transport).asBoolean();
        final String machine = ((resolvedValue = CommonAttributes.MACHINE.resolveModelAttribute(context, transport)).isDefined()) ? resolvedValue.asString() : null ;
        final String rack = ((resolvedValue = CommonAttributes.RACK.resolveModelAttribute(context, transport)).isDefined()) ? resolvedValue.asString() : null ;
        final String site = ((resolvedValue = CommonAttributes.SITE.resolveModelAttribute(context, transport)).isDefined()) ? resolvedValue.asString() : null ;
        final String timerExecutor = ((resolvedValue = CommonAttributes.TIMER_EXECUTOR.resolveModelAttribute(context, transport)).isDefined()) ? resolvedValue.asString() : null ;
        final String threadFactory = ((resolvedValue = CommonAttributes.THREAD_FACTORY.resolveModelAttribute(context, transport)).isDefined()) ? resolvedValue.asString() : null ;
        final String diagnosticsSocketBinding = ((resolvedValue = CommonAttributes.DIAGNOSTICS_SOCKET_BINDING.resolveModelAttribute(context, transport)).isDefined()) ? resolvedValue.asString() : null ;
        final String defaultExecutor = ((resolvedValue = CommonAttributes.DEFAULT_EXECUTOR.resolveModelAttribute(context, transport)).isDefined()) ? resolvedValue.asString() : null ;
        final String oobExecutor = ((resolvedValue = CommonAttributes.OOB_EXECUTOR.resolveModelAttribute(context, transport)).isDefined()) ? resolvedValue.asString() : null ;

        Transport transportConfig = new Transport(type);
        ProtocolStack stackConfig = new ProtocolStack(name, transportConfig);

        ServiceBuilder<ChannelFactory> builder = context.getServiceTarget()
                .addService(ChannelFactoryService.getServiceName(name), new ChannelFactoryService(stackConfig))
                .addDependency(ProtocolDefaultsService.SERVICE_NAME, ProtocolDefaults.class, stackConfig.getDefaultsInjector())
                .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, stackConfig.getMBeanServerInjector())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, stackConfig.getEnvironmentInjector())
                ;

        transportConfig.setShared(shared);
        if (machine != null) {
            transportConfig.setMachineId(machine);
        }
        if (rack != null) {
            transportConfig.setRackId(rack);
        }
        if (site != null) {
            transportConfig.setSiteId(site);
        }
        build(builder, context, transport, transportConfig);

        addSocketBindingDependency(builder, diagnosticsSocketBinding, transportConfig.getDiagnosticsSocketBindingInjector());
        addExecutorDependency(builder, defaultExecutor, transportConfig.getDefaultExecutorInjector());
        addExecutorDependency(builder, oobExecutor, transportConfig.getOOBExecutorInjector());
        if (timerExecutor != null) {
            builder.addDependency(ThreadsServices.executorName(timerExecutor), ScheduledExecutorService.class, transportConfig.getTimerExecutorInjector());
        }
        if (threadFactory != null) {
            builder.addDependency(ThreadsServices.threadFactoryName(threadFactory), ThreadFactory.class, transportConfig.getThreadFactoryInjector());
        }

        // pick up the protocols here as a List<Property> where property is <name, ModelNode>
        // we need to preserve the order of the protocols as maintained by PROTOCOLS
        List<Property>  orderedProtocols =  getOrderedProtocolPropertyList(model) ;
        for (Property protocol : orderedProtocols) {

            ModelNode typeModelNode = null ;
            final String theType = ((typeModelNode = CommonAttributes.TYPE.resolveModelAttribute(context, protocol.getValue())).isDefined()) ? typeModelNode.asString() : null ;

            Protocol protocolConfig = new Protocol(theType);
            build(builder, context, protocol.getValue(), protocolConfig);
            stackConfig.getProtocols().add(protocolConfig);
        }
        builder.setInitialMode(ServiceController.Mode.ON_DEMAND);

        newControllers.add(builder.install());
    }

    public static List<Property> getOrderedProtocolPropertyList(ModelNode stack) {
        ModelNode orderedProtocols = new ModelNode();

        // check for the empty ordering list
        if  (!stack.hasDefined(ModelKeys.PROTOCOLS)) {
            return null ;
        }
        // PROTOCOLS is a list of protocol names only, reflecting the order in which protocols were added to the stack
        List<ModelNode> protocolOrdering = stack.get(ModelKeys.PROTOCOLS).asList();

        // npow construct an ordered list of the full protocol model nodes
        ModelNode unorderedProtocols = stack.get(ModelKeys.PROTOCOL) ;
        for (ModelNode protocolName : protocolOrdering) {
            ModelNode protocolModel = unorderedProtocols.get(protocolName.asString());
            orderedProtocols.add(protocolName.asString(), protocolModel) ;
        }
        return orderedProtocols.asPropertyList();
    }

    private void build(ServiceBuilder<ChannelFactory> builder, OperationContext context, ModelNode protocol, Protocol protocolConfig)
            throws OperationFailedException {

        ModelNode resolvedValue = null ;
        final String socketBinding = ((resolvedValue = CommonAttributes.SOCKET_BINDING.resolveModelAttribute(context, protocol)).isDefined()) ? resolvedValue.asString() : null ;
        this.addSocketBindingDependency(builder, socketBinding, protocolConfig.getSocketBindingInjector());

        Map<String, String> properties = protocolConfig.getProperties();

        // properties are a child resource of protocol
        if (protocol.hasDefined(ModelKeys.PROPERTY)) {
            for (Property property : protocol.get(ModelKeys.PROPERTY).asPropertyList()) {
                // the format of the property elements
                //  "property" => {
                //       "relative-to" => {"value" => "fred"},
                //   }
                String propertyName = property.getName();
                Property complexValue = property.getValue().asProperty();
                String propertyValue = complexValue.getValue().asString();
                properties.put(propertyName, propertyValue);
            }
       }
    }

    private void addSocketBindingDependency(ServiceBuilder<ChannelFactory> builder, String socketBinding, Injector<SocketBinding> injector) {
        if (socketBinding != null) {
            builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(socketBinding), SocketBinding.class, injector);
        }
    }

    private void addExecutorDependency(ServiceBuilder<ChannelFactory> builder, String executor, Injector<Executor> injector) {
        if (executor != null) {
            builder.addDependency(ThreadsServices.executorName(executor), Executor.class, injector);
        }
    }

    /*
     * A check that we have the minimal configuration required to create a protocol stack.
     */
    private void protocolStackSanityCheck(String stackName, ModelNode model) throws OperationFailedException {

         ModelNode transport = model.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
         if (!transport.isDefined()) {
            throw JGroupsMessages.MESSAGES.transportNotDefined(stackName) ;
         }

         List<Property> protocols = getOrderedProtocolPropertyList(model);
         if ( protocols == null || !(protocols.size() > 0)) {
             throw JGroupsMessages.MESSAGES.protocolListNotDefined(stackName) ;
         }
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    static class ProtocolStack implements ProtocolStackConfiguration {
        private final InjectedValue<ProtocolDefaults> defaults = new InjectedValue<ProtocolDefaults>();
        private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
        private final InjectedValue<ServerEnvironment> environment = new InjectedValue<ServerEnvironment>();

        private final String name;
        private final TransportConfiguration transport;
        private final List<ProtocolConfiguration> protocols = new LinkedList<ProtocolConfiguration>();

        ProtocolStack(String name, TransportConfiguration transport) {
            this.name = name;
            this.transport = transport;
        }

        Injector<ProtocolDefaults> getDefaultsInjector() {
            return this.defaults;
        }

        Injector<MBeanServer> getMBeanServerInjector() {
            return this.mbeanServer;
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
        public MBeanServer getMBeanServer() {
            return this.mbeanServer.getOptionalValue();
        }

        @Override
        public ServerEnvironment getEnvironment() {
            return this.environment.getValue();
        }
    }

    static class Transport extends Protocol implements TransportConfiguration {
        private final InjectedValue<SocketBinding> diagnosticsSocketBinding = new InjectedValue<SocketBinding>();
        private final InjectedValue<Executor> defaultExecutor = new InjectedValue<Executor>();
        private final InjectedValue<Executor> oobExecutor = new InjectedValue<Executor>();
        private final InjectedValue<ScheduledExecutorService> timerExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ThreadFactory> threadFactory = new InjectedValue<ThreadFactory>();
        private boolean shared = true;
        private String machineId;
        private String rackId;
        private String siteId;

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
        public String getMachineId() {
            return machineId;
        }

        public void setMachineId(String machineId) {
            this.machineId = machineId;
        }

        @Override
        public String getRackId() {
            return rackId;
        }

        public void setRackId(String rackId) {
            this.rackId = rackId;
        }

        @Override
        public String getSiteId() {
            return siteId;
        }

        public void setSiteId(String siteId) {
            this.siteId = siteId;
        }

        @Override
        public boolean hasTopology() {
            return (machineId!=null || rackId!=null || siteId!=null);
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
    }

    static class Protocol implements ProtocolConfiguration {
        private final String name;
        private final InjectedValue<SocketBinding> socketBinding = new InjectedValue<SocketBinding>();
        private final Map<String, String> properties = new HashMap<String, String>();
        private final Class<?> protocolClass;

        Protocol(String name) {
            this.name = name;
            try {
                this.protocolClass = this.getClass().getClassLoader().loadClass(org.jgroups.conf.ProtocolConfiguration.protocol_prefix + '.' + name);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public boolean hasProperty(String property) {
            return getField(this.protocolClass, property) != null;
        }

        private static Field getField(Class<?> targetClass, String property) {
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
