/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jgroups.Global;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for non-transport/relay JGroups protocols.
 * @author Paul Ferraro
 */
public class AbstractProtocolResourceDefinitionRegistrar<P extends Protocol> extends ProtocolConfigurationResourceDefinitionRegistrar<P, ProtocolConfiguration<P>> {
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ProtocolConfiguration.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    static final UnaryOperator<OperationStepHandler> LEGACY_OPERATION_TRANSFORMER = new UnaryOperator<>() {
        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    PathAddress address = context.getCurrentAddress();
                    PathElement protocolPath = address.getLastElement();
                    PathElement nativeProtocolPath = StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(Global.PREFIX + protocolPath.getValue());
                    if (!protocolPath.equals(nativeProtocolPath)) {
                        PathAddress parentAddress = address.getParent();
                        Resource parent = context.readResourceFromRoot(parentAddress, false);
                        // If native protocol exists, transform this operation using the native protocol as the target resource
                        if (parent.hasChild(nativeProtocolPath)) {
                            PathAddress nativeAddress = parentAddress.append(nativeProtocolPath);
                            operation.get(ModelDescriptionConstants.OP_ADDR).set(nativeAddress.toModelNode());
                            String operationName = operation.get(ModelDescriptionConstants.OP).asString();
                            context.addStep(operation, context.getRootResourceRegistration().getOperationHandler(nativeAddress, operationName), OperationContext.Stage.MODEL, true);
                            return;
                        }
                    }
                    handler.execute(context, operation);
                }
            };
        }
    };

    static class LegacyAddOperationTransformation implements UnaryOperator<OperationStepHandler> {
        private final Predicate<ModelNode> legacy;

        <E extends Enum<E> & AttributeDefinitionProvider> LegacyAddOperationTransformation(Class<E> attributeClass) {
            this(EnumSet.allOf(attributeClass), AttributeDefinitionProvider::get);
        }

        LegacyAddOperationTransformation(Collection<AttributeDefinition> attributes) {
            this(attributes, Function.identity());
        }

        private <E> LegacyAddOperationTransformation(Collection<E> elements, Function<E, AttributeDefinition> mapper) {
            // If none of the specified attributes are defined, then this is a legacy operation
            this.legacy = operation -> {
                for (E element : elements) {
                    AttributeDefinition attribute = mapper.apply(element);
                    if (operation.hasDefined(attribute.getName())) return false;
                }
                return true;
            };
        }

        LegacyAddOperationTransformation(String... legacyProperties) {
            // If any of the specified properties are defined, then this is a legacy operation
            this.legacy = operation -> {
                if (!operation.hasDefined(ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName())) return false;
                for (String legacyProperty : legacyProperties) {
                    if (operation.get(ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName()).hasDefined(legacyProperty)) return true;
                }
                return false;
            };
        }

        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return (context, operation) -> {
                if (this.legacy.test(operation)) {
                    PathElement path = context.getCurrentAddress().getLastElement();
                    // This is a legacy add operation - process it using the generic handler
                    OperationStepHandler genericHandler = context.getResourceRegistration().getParent().getOperationHandler(PathAddress.pathAddress(StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement()), ModelDescriptionConstants.ADD);
                    operation.get(ModelDescriptionConstants.OP_ADDR).set(context.getCurrentAddress().getParent().append(StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(Global.PREFIX + path.getValue())).toModelNode());
                    // Process this step first to preserve protocol order
                    context.addStep(operation, genericHandler, OperationContext.Stage.MODEL, true);
                } else {
                    handler.execute(context, operation);
                }
            };
        }
    }

    interface Configurator extends ProtocolConfigurationResourceDefinitionRegistrar.Configurator {
        @Override
        default RuntimeCapability<Void> getCapability() {
            return CAPABILITY;
        }

        @Override
        default ResourceDescriptionResolver getResourceDescriptionResolver() {
            PathElement path = this.getResourceRegistration().getPathElement();
            return path.isWildcard() ? JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(path) : JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(path, StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement());
        }

        @Override
        default UnaryOperator<ResourceDefinition.Builder> getResourceDefinitionConfigurator() {
            return ResourceDefinition.Builder::asOrderedChild;
        }
    }

    AbstractProtocolResourceDefinitionRegistrar(Configurator registration) {
        super(registration);
    }

    @Override
    public ServiceDependency<ProtocolConfiguration<P>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return this.resolver.resolve(context, model);
    }
}
