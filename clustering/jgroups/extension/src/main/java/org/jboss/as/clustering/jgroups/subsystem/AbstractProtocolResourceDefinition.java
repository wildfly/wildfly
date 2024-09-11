/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ModuleNameValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jgroups.Global;
import org.jgroups.stack.Protocol;
import org.jgroups.util.StackType;
import org.jgroups.util.Util;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.common.function.Functions;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Resource description for /subsystem=jgroups/stack=X/protocol=Y
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public abstract class AbstractProtocolResourceDefinition<P extends Protocol, C extends ProtocolConfiguration<P>> extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator, ResourceModelResolver<Map.Entry<Function<ProtocolConfiguration<P>, C>, Consumer<RequirementServiceBuilder<?>>>> {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        MODULE(ModelDescriptionConstants.MODULE, ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode("org.jgroups"))
                        .setValidator(ModuleNameValidator.INSTANCE)
                        ;
            }
        },
        PROPERTIES(ModelDescriptionConstants.PROPERTIES),
        STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        Attribute(String name) {
            this.definition = new PropertiesAttributeDefinition.Builder(name)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    private final RuntimeCapability<Void> capability;
    private final UnaryOperator<ResourceDescriptor> resourceConfigurator;
    private final ResourceServiceConfigurator parentServiceConfigurator;

    AbstractProtocolResourceDefinition(Parameters parameters, RuntimeCapability<Void> capability, UnaryOperator<ResourceDescriptor> resourceConfigurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(parameters);
        this.capability = capability;
        this.resourceConfigurator = resourceConfigurator;
        this.parentServiceConfigurator = parentServiceConfigurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = this.resourceConfigurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addCapabilities(List.of(this.capability))
                .addAttributes(Attribute.class)
                ;
        List<ResourceOperationRuntimeHandler> handlers = new ArrayList<>(2);
        handlers.add(ResourceOperationRuntimeHandler.configureService(this));
        if (this.parentServiceConfigurator != null) {
            handlers.add(ResourceOperationRuntimeHandler.restartParent(ResourceOperationRuntimeHandler.configureService(this.parentServiceConfigurator)));
        }

        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handlers)).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        Boolean statisticsEnabled = Attribute.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBooleanOrNull();
        String moduleName = Attribute.MODULE.resolveModelAttribute(context, model).asString();
        boolean nativeProtocol = moduleName.equals(Attribute.MODULE.getDefinition().getDefaultValue().asString()) && !name.startsWith(Global.PREFIX);
        // A "native" protocol is one that is not specified as a class name
        String className = nativeProtocol ? (Global.PREFIX + name) : name;

        Map<String, String> properties = new TreeMap<>();
        for (Property property : Attribute.PROPERTIES.resolveModelAttribute(context, model).asPropertyListOrEmpty()) {
            properties.put(property.getName(), property.getValue().asString());
        }

        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        ServiceDependency<ProtocolDefaults> defaults = ServiceDependency.on(ProtocolDefaults.SERVICE_NAME);

        ProtocolConfiguration<P> configuration = new ProtocolConfiguration<>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public P createProtocol(ProtocolStackConfiguration stackConfiguration) {
                try {
                    Module module = loader.get().loadModule(moduleName);
                    Class<? extends Protocol> protocolClass = module.getClassLoader().loadClass(className).asSubclass(Protocol.class);
                    Map<String, String> protocolProperties = new HashMap<>(defaults.get().getProperties(protocolClass));
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
                    protocol.enableStats(statisticsEnabled != null ? statisticsEnabled : stackConfiguration.isStatisticsEnabled());
                    return protocol;
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        Map.Entry<Function<ProtocolConfiguration<P>, C>, Consumer<RequirementServiceBuilder<?>>> entry = this.resolve(context, model);
        return CapabilityServiceInstaller.builder(this.capability, entry.getKey(), Functions.constantSupplier(configuration)).blocking()
                .requires(List.of(loader, defaults, entry.getValue()))
                .build();
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
        public P createProtocol(ProtocolStackConfiguration stackConfiguration) {
            return this.configuration.createProtocol(stackConfiguration);
        }

        @Override
        public Map<String, SocketBinding> getSocketBindings() {
            return this.configuration.getSocketBindings();
        }

        void setValue(P protocol, String propertyName, Object propertyValue) {
            PrivilegedAction<P> action = new PrivilegedAction<>() {
                @Override
                public P run() {
                    return protocol.setValue(propertyName, propertyValue);
                }
            };
            WildFlySecurityManager.doUnchecked(action);
        }
    }
}
