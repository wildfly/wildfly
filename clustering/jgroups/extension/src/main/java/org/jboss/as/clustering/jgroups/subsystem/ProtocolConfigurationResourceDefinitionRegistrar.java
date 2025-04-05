/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.jgroups.ProtocolPropertiesRepository;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jgroups.Global;
import org.jgroups.stack.Protocol;
import org.jgroups.util.StackType;
import org.jgroups.util.Util;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for a JGroups protocol.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public abstract class ProtocolConfigurationResourceDefinitionRegistrar<P extends Protocol, C extends ProtocolConfiguration<P>> extends ProtocolChildResourceDefinitionRegistrar implements ResourceServiceConfigurator, ResourceModelResolver<ServiceDependency<C>> {

    interface Configurator extends ProtocolChildResourceDefinitionRegistrar.Configurator {
        RuntimeCapability<Void> getCapability();

        ResourceOperationRuntimeHandler getParentRuntimeHandler();
    }

    ResourceModelResolver<ServiceDependency<ProtocolConfiguration<P>>> resolver = new ResourceModelResolver<>() {
        @Override
        public ServiceDependency<ProtocolConfiguration<P>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
            String name = context.getCurrentAddressValue();

            ServiceDependency<Module> module = MODULE.resolve(context, model);
            Map<String, String> properties = PROPERTIES.resolve(context, model);
            Boolean statisticsEnabled = STATISTICS_ENABLED.resolve(context, model);
            ServiceDependency<ProtocolPropertiesRepository> repository = ServiceDependency.on(ProtocolPropertiesRepository.SERVICE_DESCRIPTOR);
            return new ServiceDependency<>() {
                @Override
                public void accept(RequirementServiceBuilder<?> builder) {
                    module.accept(builder);
                    repository.accept(builder);
                }

                @Override
                public ProtocolConfiguration<P> get() {
                    return new ProtocolConfiguration<>() {
                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public P createProtocol(ChannelFactoryConfiguration configuration) {
                            try {
                                // A "native" protocol is one that is not specified as a class name
                                boolean nativeProtocol = module.get().getName().equals(MODULE.getDefaultValue().asString()) && !name.startsWith(Global.PREFIX);
                                String className = nativeProtocol ? (Global.PREFIX + name) : name;
                                Class<? extends Protocol> protocolClass = module.get().getClassLoader().loadClass(className).asSubclass(Protocol.class);
                                Map<String, String> protocolProperties = new HashMap<>(repository.get().getProperties(protocolClass));
                                protocolProperties.putAll(properties);
                                PrivilegedExceptionAction<Protocol> action = new PrivilegedExceptionAction<>() {
                                    @Override
                                    public Protocol run() throws Exception {
                                        try {
                                            Protocol protocol = protocolClass.getConstructor().newInstance();
                                            // These Configurator methods are destructive, so make a defensive copy
                                            Map<String, String> copy = new HashMap<>(protocolProperties);
                                            StackType type = Util.getIpStackType();
                                            org.jgroups.stack.Configurator.resolveAndAssignFields(protocol, copy, type);
                                            org.jgroups.stack.Configurator.resolveAndInvokePropertyMethods(protocol, copy, type);
                                            List<Object> objects = protocol.getComponents();
                                            if (objects != null) {
                                                for (Object object : objects) {
                                                    org.jgroups.stack.Configurator.resolveAndAssignFields(object, copy, type);
                                                    org.jgroups.stack.Configurator.resolveAndInvokePropertyMethods(object, copy, type);
                                                }
                                            }
                                            if (!copy.isEmpty()) {
                                                for (String property : copy.keySet()) {
                                                    JGroupsLogger.ROOT_LOGGER.unrecognizedProtocolProperty(name, property);
                                                }
                                            }
                                            return protocol;
                                        } catch (InstantiationException | IllegalAccessException e) {
                                            throw new IllegalStateException(e);
                                        }
                                    }
                                };
                                @SuppressWarnings("unchecked")
                                P protocol = (P) WildFlySecurityManager.doUnchecked(action);
                                protocol.enableStats(statisticsEnabled != null ? statisticsEnabled : configuration.isStatisticsEnabled());
                                return protocol;
                            } catch (Exception e) {
                                throw new IllegalArgumentException(e);
                            }
                        }
                    };
                }
            };
        }
    };

    private final RuntimeCapability<Void> capability;
    private final ResourceOperationRuntimeHandler parentRuntimeHandler;

    ProtocolConfigurationResourceDefinitionRegistrar(Configurator configurator) {
        super(configurator);
        this.capability = configurator.getCapability();
        this.parentRuntimeHandler = configurator.getParentRuntimeHandler();
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addCapability(this.capability)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.combine(ResourceOperationRuntimeHandler.configureService(this), ResourceOperationRuntimeHandler.restartParent(this.parentRuntimeHandler)))
                ;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(this.capability, this.resolve(context, model)).blocking().build();
    }

    static class ProtocolConfigurationDecorator<P extends Protocol> implements ProtocolConfiguration<P> {
        private final ProtocolConfiguration<P> configuration;

        ProtocolConfigurationDecorator(ProtocolConfiguration<P> configuration) {
            this.configuration = configuration;
        }

        @Override
        public String getName() {
            return this.configuration.getName();
        }

        @Override
        public P createProtocol(ChannelFactoryConfiguration configuration) {
            return this.configuration.createProtocol(configuration);
        }

        @Override
        public Map<String, SocketBinding> getSocketBindings() {
            return this.configuration.getSocketBindings();
        }

        P setValue(P protocol, String propertyName, Object propertyValue) {
            PrivilegedAction<P> action = new PrivilegedAction<>() {
                @Override
                public P run() {
                    return protocol.setValue(propertyName, propertyValue);
                }
            };
            return WildFlySecurityManager.doUnchecked(action);
        }
    }
}
