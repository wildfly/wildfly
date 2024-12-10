/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public interface TransportResourceDescription extends ProtocolChildResourceDescription {
    TransportResourceDescription INSTANCE = of(PathElement.WILDCARD_VALUE);

    RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(TransportConfiguration.SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("transport", name);
    }

    static TransportResourceDescription of(String name) {
        PathElement path = pathElement(name);
        return new TransportResourceDescription() {
            @Override
            public PathElement getPathElement() {
                return path;
            }
        };
    }

    enum SocketBindingAttribute implements AttributeDefinitionProvider, ResourceModelResolver<ServiceDependency<SocketBinding>> {
        SERVER(ModelDescriptionConstants.SOCKET_BINDING, true),
        CLIENT("client-socket-binding", false),
        DIAGNOSTICS("diagnostics-socket-binding", false),
        ;
        private final CapabilityReferenceAttributeDefinition<SocketBinding> attribute;

        SocketBindingAttribute(String name, boolean required) {
            this.attribute = new CapabilityReferenceAttributeDefinition.Builder<>(name, CapabilityReference.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
                    .setRequired(required)
                    .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                    .build();
        }

        @Override
        public ServiceDependency<SocketBinding> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
            return this.attribute.resolve(context, model);
        }

        @Override
        public AttributeDefinition get() {
            return this.attribute;
        }
    }

    enum TopologyAttribute implements AttributeDefinitionProvider, ResourceModelResolver<String> {
        SITE("site"),
        RACK("rack"),
        MACHINE("machine"),
        ;
        private final AttributeDefinition definition;

        TopologyAttribute(String name) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, ModelType.STRING)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }

        @Override
        public String resolve(OperationContext context, ModelNode model) throws OperationFailedException {
            return this.resolveModelAttribute(context, model).asStringOrNull();
        }
    }

    @Override
    default PathElement getPathKey() {
        return INSTANCE.getPathElement();
    }

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.concat(ResourceDescriptor.stream(EnumSet.complementOf(EnumSet.of(SocketBindingAttribute.CLIENT))), ResourceDescriptor.stream(EnumSet.allOf(TopologyAttribute.class))), ProtocolChildResourceDescription.super.getAttributes());
    }
}
