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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.ProtocolConfiguration;
import org.jboss.as.clustering.jgroups.ProtocolStackConfiguration;
import org.jboss.as.clustering.jgroups.TransportConfiguration;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class ProtocolStackAdd implements ModelAddOperationHandler, DescriptionProvider {

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) {
        target.get(ModelKeys.TRANSPORT).set(source.require(ModelKeys.TRANSPORT));
        ModelNode protocols = target.get(ModelKeys.PROTOCOL);
        for (ModelNode protocol: source.require(ModelKeys.PROTOCOL).asList()) {
            protocols.add(protocol);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return LocalDescriptions.getProtocolStackAddDescription(locale);
    }

    @Override
    public OperationResult execute(OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

        ModelNode opAddr = operation.require(ModelDescriptionConstants.OP_ADDR);

        final ModelNode removeOperation = Util.getResourceRemoveOperation(opAddr);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        populate(operation, context.getSubModel());

        RuntimeOperationContext runtime = context.getRuntimeContext();
        if (runtime != null) {
            RuntimeTask task = new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    ModelNode transport = operation.get(ModelKeys.TRANSPORT);
                    TransportConfigurationImpl transportConfig = new TransportConfigurationImpl(transport.require(ModelKeys.TYPE).asString());
                    ProtocolStackConfigurationImpl stackConfig = new ProtocolStackConfigurationImpl(transportConfig);
                    InjectionCollector injections = new InjectionCollector();
                    this.process(transport, transportConfig, injections);
                    if (transport.has(ModelKeys.DIAGNOSTICS_SOCKET_BINDING)) {
                        injections.addSocketBindingInjector(transport.get(ModelKeys.DIAGNOSTICS_SOCKET_BINDING).asString(), transportConfig.getDiagnosticsSocketBindingInjector());
                    }
                    if (transport.has(ModelKeys.THREAD_POOL)) {
                        injections.addExecutorInjector(transport.get(ModelKeys.THREAD_POOL).asString(), transportConfig.getThreadPoolInjector());
                    }
                    if (transport.has(ModelKeys.OOB_THREAD_POOL)) {
                        injections.addExecutorInjector(transport.get(ModelKeys.OOB_THREAD_POOL).asString(), transportConfig.getOOBThreadPoolInjector());
                    }
                    if (transport.has(ModelKeys.TIMER_THREAD_POOL)) {
                        injections.addScheduledExecutorInjector(transport.get(ModelKeys.TIMER_THREAD_POOL).asString(), transportConfig.getTimerThreadPoolInjector());
                    }
                    if (transport.has(ModelKeys.THREAD_FACTORY)) {
                        injections.addThreadFactoryInjector(transport.get(ModelKeys.THREAD_FACTORY).asString(), transportConfig.getThreadFactoryInjector());
                    }
                    for (ModelNode protocol: operation.get(ModelKeys.PROTOCOL).asList()) {
                        ProtocolConfigurationImpl protocolConfig = new ProtocolConfigurationImpl(protocol.require(ModelKeys.TYPE).asString());
                        this.process(protocol, protocolConfig, injections);
                        stackConfig.getProtocols().add(protocolConfig);
                    }
                    ServiceBuilder<ChannelFactory> builder = new ChannelFactoryService(name, stackConfig).build(context.getServiceTarget());
                    injections.addDependencies(builder);
                    builder.addListener(new ResultHandler.ServiceStartListener(resultHandler));
                    builder.install();
                }

                private void process(ModelNode protocol, ProtocolConfigurationImpl protocolConfig, InjectionCollector injections) {
                    if (protocol.has(ModelKeys.SOCKET_BINDING)) {
                        injections.addSocketBindingInjector(protocol.get(ModelKeys.SOCKET_BINDING).asString(), protocolConfig.getSocketBindingInjector());
                    }

                    Map<String, String> properties = protocolConfig.getProperties();
                    if (protocol.has(ModelKeys.PROPERTY)) {
                        for (Property property: protocol.get(ModelKeys.PROPERTY).asPropertyList()) {
                            properties.put(property.getName(), property.getValue().asString());
                        }
                    }
                }
            };
            runtime.setRuntimeTask(task);
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(removeOperation);
    }

    static class InjectionCollector {
        List<Map.Entry<String, Injector<SocketBinding>>> socketBindingInjections = new LinkedList<Map.Entry<String, Injector<SocketBinding>>>();
        List<Map.Entry<String, Injector<Executor>>> executorInjections = new LinkedList<Map.Entry<String, Injector<Executor>>>();
        List<Map.Entry<String, Injector<ScheduledExecutorService>>> scheduledExecutorInjections = new LinkedList<Map.Entry<String, Injector<ScheduledExecutorService>>>();
        List<Map.Entry<String, Injector<ThreadFactory>>> threadFactoryInjections = new LinkedList<Map.Entry<String, Injector<ThreadFactory>>>();

        void addSocketBindingInjector(String name, Injector<SocketBinding> injector) {
            this.socketBindingInjections.add(new AbstractMap.SimpleImmutableEntry<String, Injector<SocketBinding>>(name, injector));
        }

        void addExecutorInjector(String name, Injector<Executor> injector) {
            this.executorInjections.add(new AbstractMap.SimpleImmutableEntry<String, Injector<Executor>>(name, injector));
        }

        void addScheduledExecutorInjector(String name, Injector<ScheduledExecutorService> injector) {
            this.scheduledExecutorInjections.add(new AbstractMap.SimpleImmutableEntry<String, Injector<ScheduledExecutorService>>(name, injector));
        }

        void addThreadFactoryInjector(String name, Injector<ThreadFactory> injector) {
            this.threadFactoryInjections.add(new AbstractMap.SimpleImmutableEntry<String, Injector<ThreadFactory>>(name, injector));
        }

        void addDependencies(ServiceBuilder<?> builder) {
            for (Map.Entry<String, Injector<SocketBinding>> injection: this.socketBindingInjections) {
                builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(injection.getKey()), SocketBinding.class, injection.getValue());
            }
            for (Map.Entry<String, Injector<Executor>> injection: this.executorInjections) {
                builder.addDependency(ThreadsServices.executorName(injection.getKey()), Executor.class, injection.getValue());
            }
            for (Map.Entry<String, Injector<ScheduledExecutorService>> injection: this.scheduledExecutorInjections) {
                builder.addDependency(ThreadsServices.executorName(injection.getKey()), ScheduledExecutorService.class, injection.getValue());
            }
            for (Map.Entry<String, Injector<ThreadFactory>> injection: this.threadFactoryInjections) {
                builder.addDependency(ThreadsServices.threadFactoryName(injection.getKey()), ThreadFactory.class, injection.getValue());
            }
        }
    }

    static class ProtocolStackConfigurationImpl implements ProtocolStackConfiguration {
        private final TransportConfiguration transport;
        private final List<ProtocolConfiguration> protocols = new LinkedList<ProtocolConfiguration>();

        ProtocolStackConfigurationImpl(TransportConfiguration transport) {
            this.transport = transport;
        }

        @Override
        public TransportConfiguration getTransport() {
            return this.transport;
        }

        @Override
        public List<ProtocolConfiguration> getProtocols() {
            return this.protocols;
        }
    }

    static class TransportConfigurationImpl extends ProtocolConfigurationImpl implements TransportConfiguration {
        private final InjectedValue<SocketBinding> diagnosticsSocketBinding = new InjectedValue<SocketBinding>();
        private final InjectedValue<Executor> threadPool = new InjectedValue<Executor>();
        private final InjectedValue<Executor> oobThreadPool = new InjectedValue<Executor>();
        private final InjectedValue<ScheduledExecutorService> timerThreadPool = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ThreadFactory> threadFactory = new InjectedValue<ThreadFactory>();
        private boolean shared = true;

        TransportConfigurationImpl(String name) {
            super(name);
        }

        public Injector<SocketBinding> getDiagnosticsSocketBindingInjector() {
            return this.diagnosticsSocketBinding;
        }

        public Injector<Executor> getThreadPoolInjector() {
            return this.threadPool;
        }

        public Injector<Executor> getOOBThreadPoolInjector() {
            return this.oobThreadPool;
        }

        public Injector<ScheduledExecutorService> getTimerThreadPoolInjector() {
            return this.timerThreadPool;
        }

        public Injector<ThreadFactory> getThreadFactoryInjector() {
            return this.threadFactory;
        }

        public boolean isShared() {
            return this.shared;
        }

        public void setShared(boolean shared) {
            this.shared = shared;
        }

        @Override
        public SocketBinding getDiagnosticsSocketBinding() {
            return this.diagnosticsSocketBinding.getOptionalValue();
        }

        @Override
        public Executor getThreadPool() {
            return this.threadPool.getOptionalValue();
        }

        @Override
        public Executor getOOBThreadPool() {
            return this.oobThreadPool.getOptionalValue();
        }

        @Override
        public ScheduledExecutorService getTimerThreadPool() {
            return this.timerThreadPool.getOptionalValue();
        }

        @Override
        public ThreadFactory getThreadFactory() {
            return this.threadFactory.getOptionalValue();
        }
    }

    static class ProtocolConfigurationImpl implements ProtocolConfiguration {
        private final String name;
        private final InjectedValue<SocketBinding> socketBinding = new InjectedValue<SocketBinding>();
        private final Map<String, String> properties = new HashMap<String, String>();

        ProtocolConfigurationImpl(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public Map<String, String> getProperties() {
            return this.properties;
        }

        public Injector<SocketBinding> getSocketBindingInjector() {
            return this.socketBinding;
        }

        @Override
        public SocketBinding getSocketBinding() {
            return this.socketBinding.getOptionalValue();
        }
    }
}
