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
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.clustering.jgroups.ProtocolConfiguration;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.clustering.jgroups.ProtocolStackConfiguration;
import org.jboss.as.clustering.jgroups.TransportConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossExecutors;

/**
 * @author Paul Ferraro
 */
public class ProtocolStackAdd extends AbstractAddStepHandler implements DescriptionProvider {

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) {
        target.get(ModelKeys.TRANSPORT).set(source.require(ModelKeys.TRANSPORT));
        ModelNode protocols = target.get(ModelKeys.PROTOCOL);
        for (ModelNode protocol : source.require(ModelKeys.PROTOCOL).asList()) {
            protocols.add(protocol);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return LocalDescriptions.getProtocolStackAddDescription(locale);
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        populate(operation, model);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        ModelNode transport = operation.get(ModelKeys.TRANSPORT);
        Transport transportConfig = new Transport(transport.require(ModelKeys.TYPE).asString());
        ProtocolStack stackConfig = new ProtocolStack(name, transportConfig);

        ServiceBuilder<ChannelFactory> builder = context.getServiceTarget()
                .addService(ChannelFactoryService.getServiceName(name), new ValueService<ChannelFactory>(new ImmediateValue<ChannelFactory>(new JChannelFactory(stackConfig))))
                .addDependency(ProtocolDefaultsService.SERVICE_NAME, ProtocolDefaults.class, stackConfig.getDefaultsInjector())
                .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, stackConfig.getMBeanServerInjector());
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
        for (ModelNode protocol : operation.get(ModelKeys.PROTOCOL).asList()) {
            Protocol protocolConfig = new Protocol(protocol.require(ModelKeys.TYPE).asString());
            build(builder, protocol, protocolConfig);
            stackConfig.getProtocols().add(protocolConfig);
        }
        builder.setInitialMode(ServiceController.Mode.ON_DEMAND);

        newControllers.add(builder.install());

    }

    private void build(ServiceBuilder<ChannelFactory> builder, ModelNode protocol, Protocol protocolConfig) {
        this.addSocketBindingDependency(builder, protocol, ModelKeys.SOCKET_BINDING, protocolConfig.getSocketBindingInjector());
        Map<String, String> properties = protocolConfig.getProperties();
        if (protocol.hasDefined(ModelKeys.PROPERTY)) {
            for (Property property : protocol.get(ModelKeys.PROPERTY).asPropertyList()) {
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

    protected boolean requiresRuntimeVerification() {
        return false;
    }

    static class ProtocolStack implements ProtocolStackConfiguration {
        private final InjectedValue<ProtocolDefaults> defaults = new InjectedValue<ProtocolDefaults>();
        private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();

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
    }

    static class Transport extends Protocol implements TransportConfiguration {
        private final InjectedValue<SocketBinding> diagnosticsSocketBinding = new InjectedValue<SocketBinding>();
        private final InjectedValue<Executor> defaultExecutor = new InjectedValue<Executor>();
        private final InjectedValue<Executor> oobExecutor = new InjectedValue<Executor>();
        private final InjectedValue<ScheduledExecutorService> timerExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ThreadFactory> threadFactory = new InjectedValue<ThreadFactory>();
        private boolean shared = true;

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

        Protocol(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
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
