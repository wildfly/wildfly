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
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a socket-based protocol.
 * @author Paul Ferraro
 */
public class SocketProtocolResourceDefinitionRegistrar<P extends Protocol> extends AbstractProtocolResourceDefinitionRegistrar<P> {

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

    static Stream<AttributeDefinition> attributes() {
        return Stream.concat(ResourceDescriptor.stream(EnumSet.allOf(SocketBindingAttribute.class)), AbstractProtocolResourceDefinitionRegistrar.attributes());
    }

    interface SocketProtocolResourceRegistration<P extends Protocol> extends ProtocolResourceRegistration<P> {
        @Override
        default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
            return ProtocolResourceRegistration.super.apply(builder)
                    .provideAttributes(EnumSet.allOf(SocketBindingAttribute.class))
                    ;
        }
    }

    SocketProtocolResourceDefinitionRegistrar(SocketProtocolResourceRegistration<P> registration) {
        super(registration);
    }
}
