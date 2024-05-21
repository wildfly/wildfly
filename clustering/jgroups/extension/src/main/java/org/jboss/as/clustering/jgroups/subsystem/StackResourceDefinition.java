/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Resource description for the addressable resource /subsystem=jgroups/stack=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class StackResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("stack", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(ModelNode.FALSE);
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static final RuntimeCapability<Void> CHANNEL_FACTORY = RuntimeCapability.Builder.of(ChannelFactory.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    private final ResourceServiceConfigurator configurator = JChannelFactoryServiceConfigurator.INSTANCE;

    // registration
    public StackResourceDefinition() {
        super(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(CHANNEL_FACTORY))
                ;
        ResourceServiceHandler handler = ResourceServiceHandler.of(ResourceOperationRuntimeHandler.configureService(this));
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new StackOperationHandler().register(registration);
        }

        TransportResourceRegistrar.INSTANCE.register(registration);
        new ProtocolResourceRegistrar(this.configurator).register(registration);
        new RelayResourceDefinition().register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        ResourceServiceInstaller installer = this.configurator.configure(context, model);
        ResourceServiceInstaller binderInstaller = new BinderServiceInstaller(JGroupsBindingFactory.createChannelFactoryBinding(name), context.getCapabilityServiceName(ChannelFactory.SERVICE_DESCRIPTOR, name));

        return ResourceServiceInstaller.combine(installer, binderInstaller);
    }
}
