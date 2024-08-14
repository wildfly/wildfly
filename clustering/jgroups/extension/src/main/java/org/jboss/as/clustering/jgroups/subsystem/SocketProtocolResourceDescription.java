/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 *
 */
public enum SocketProtocolResourceDescription implements ProtocolResourceDescription, Function<ResourceOperationRuntimeHandler, ChildResourceDefinitionRegistrar> {
    FD_SOCK() {
        @Override
        public ChildResourceDefinitionRegistrar apply(ResourceOperationRuntimeHandler runtimeHandler) {
            return new LegacyFailureDetectionProtocolResourceDefinitionRegistrar(this, runtimeHandler);
        }
    },
    FD_SOCK2() {
        @Override
        public ChildResourceDefinitionRegistrar apply(ResourceOperationRuntimeHandler runtimeHandler) {
            return new FailureDetectionProtocolResourceDefinitionRegistrar(this, runtimeHandler);
        }
    },
    ;

    enum SocketBindingAttribute implements AttributeDefinitionProvider, ResourceModelResolver<ServiceDependency<SocketBinding>> {
        SERVER(ModelDescriptionConstants.SOCKET_BINDING),
        CLIENT("client-socket-binding")
        ;
        private final CapabilityReferenceAttributeDefinition<SocketBinding> attribute;

        SocketBindingAttribute(String name) {
            this.attribute = new CapabilityReferenceAttributeDefinition.Builder<>(name, CapabilityReference.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
                    .setRequired(false)
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

    private final PathElement path = ProtocolResourceDescription.pathElement(this.name());

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(EnumSet.allOf(SocketBindingAttribute.class).stream().map(AttributeDefinitionProvider::get), ProtocolResourceDescription.super.getAttributes());
    }
}
