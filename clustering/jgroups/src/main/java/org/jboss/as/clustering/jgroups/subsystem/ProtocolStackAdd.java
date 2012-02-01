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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.management.MBeanServer;

import com.sun.tools.internal.xjc.model.Model;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.ProtocolConfiguration;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.clustering.jgroups.ProtocolStackConfiguration;
import org.jboss.as.clustering.jgroups.TransportConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
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
        // nothing to do
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        populate(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        // Because we use child resources in a read-only manner to configure the protocol stack, replace the local model with the full model
        model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        // pick up the transport here
        ModelNode transport = model.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);

        Transport transportConfig = new Transport(transport.require(ModelKeys.TYPE).asString());
        ProtocolStack stackConfig = new ProtocolStack(name, transportConfig);

        ServiceBuilder<ChannelFactory> builder = context.getServiceTarget()
                .addService(ChannelFactoryService.getServiceName(name), new ChannelFactoryService(stackConfig))
                .addDependency(ProtocolDefaultsService.SERVICE_NAME, ProtocolDefaults.class, stackConfig.getDefaultsInjector())
                .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, stackConfig.getMBeanServerInjector())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, stackConfig.getEnvironmentInjector())
                ;
        if (transport.hasDefined(ModelKeys.SHARED)) {
            transportConfig.setShared(transport.get(ModelKeys.SHARED).asBoolean());
        }
        if (transport.hasDefined(ModelKeys.MACHINE)) {
            transportConfig.setMachineId(context.resolveExpressions(transport.get(ModelKeys.MACHINE)).asString());
        }
        if (transport.hasDefined(ModelKeys.RACK)) {
            transportConfig.setRackId(context.resolveExpressions(transport.get(ModelKeys.RACK)).asString());
        }
        if (transport.hasDefined(ModelKeys.SITE)) {
            transportConfig.setSiteId(context.resolveExpressions(transport.get(ModelKeys.SITE)).asString());
        }
        build(builder, transport, transportConfig);
        addSocketBindingDependency(builder, transport, ModelKeys.DIAGNOSTICS_SOCKET_BINDING, transportConfig.getDiagnosticsSocketBindingInjector());
        addExecutorDependency(builder, transport, ModelKeys.DEFAULT_EXECUTOR, transportConfig.getDefaultExecutorInjector());
        addExecutorDependency(builder, transport, ModelKeys.OOB_EXECUTOR, transportConfig.getOOBExecutorInjector());
        if (transport.hasDefined(ModelKeys.TIMER_EXECUTOR)) {
            builder.addDependency(ThreadsServices.executorName(transport.get(ModelKeys.TIMER_EXECUTOR).asString()), ScheduledExecutorService.class, transportConfig.getTimerExecutorInjector());
        }
        if (transport.hasDefined(ModelKeys.THREAD_FACTORY)) {
            builder.addDependency(ThreadsServices.threadFactoryName(transport.get(ModelKeys.THREAD_FACTORY).asString()), ThreadFactory.class, transportConfig.getThreadFactoryInjector());
        }

        // pick up the protocols here as a List<Property> where property is <name, ModelNode>
        // we need to preserve the order of the protocols as maintained by PROTOCOLS
        List<Property>  orderedProtocols =  getOrderedProtocolPropertyList(model) ;
        for (Property protocol : orderedProtocols) {
            Protocol protocolConfig = new Protocol(protocol.getValue().require(ModelKeys.TYPE).asString());
            build(builder, protocol.getValue(), protocolConfig);
            stackConfig.getProtocols().add(protocolConfig);
        }
        builder.setInitialMode(ServiceController.Mode.ON_DEMAND);

        newControllers.add(builder.install());
    }

    public static List<Property> getOrderedProtocolPropertyList(ModelNode stack) {
        ModelNode orderedProtocols = new ModelNode();
        List<ModelNode> protocolOrdering = stack.get(ModelKeys.PROTOCOLS).asList();
        ModelNode unorderedProtocols = stack.get(ModelKeys.PROTOCOL) ;
        for (ModelNode protocolName : protocolOrdering) {
            ModelNode protocolModel = unorderedProtocols.get(protocolName.asString());
            orderedProtocols.add(protocolName.asString(), protocolModel) ;
        }
        return orderedProtocols.asPropertyList();
    }

    private void build(ServiceBuilder<ChannelFactory> builder, ModelNode protocol, Protocol protocolConfig) {
        this.addSocketBindingDependency(builder, protocol, ModelKeys.SOCKET_BINDING, protocolConfig.getSocketBindingInjector());
        Map<String, String> properties = protocolConfig.getProperties();
        if (protocol.hasDefined(ModelKeys.PROPERTIES)) {
            for (Property property : protocol.get(ModelKeys.PROPERTIES).asPropertyList()) {
                properties.put(property.getName(), property.getValue().asString());
            }
        }
    }

    private void addSocketBindingDependency(ServiceBuilder<ChannelFactory> builder, ModelNode model, String key, Injector<SocketBinding> injector) {
        if (model.hasDefined(key)) {
            builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(model.get(key).asString()), SocketBinding.class, injector);
        }
    }

    private void addExecutorDependency(ServiceBuilder<ChannelFactory> builder, ModelNode model, String key, Injector<Executor> injector) {
        if (model.hasDefined(key)) {
            builder.addDependency(ThreadsServices.executorName(model.get(key).asString()), Executor.class, injector);
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
