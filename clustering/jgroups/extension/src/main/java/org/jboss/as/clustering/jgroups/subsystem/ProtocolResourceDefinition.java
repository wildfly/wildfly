/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.BinaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class ProtocolResourceDefinition extends AbstractProtocolResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("protocol", name);
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        PROTOCOL("org.wildfly.clustering.jgroups.protocol"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).setDynamicNameMapper(BinaryCapabilityNameResolver.PARENT_CHILD).setAllowMultipleRegistrations(true).build();
        }

        @Override
        public RuntimeCapability<?> getDefinition() {
            return this.definition;
        }
    }

    static final UnaryOperator<OperationStepHandler> LEGACY_OPERATION_TRANSFORMER = new UnaryOperator<>() {
        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    PathAddress address = context.getCurrentAddress();
                    PathAddress parentAddress = address.getParent();
                    Resource parent = context.readResourceFromRoot(parentAddress, false);
                    PathElement legacyPath = GenericProtocolResourceDefinition.pathElement(address.getLastElement().getValue());
                    // If legacy protocol exists, transform this operation using the legacy protocol as the target resource
                    if (parent.hasChild(legacyPath)) {
                        PathAddress legacyAddress = parentAddress.append(legacyPath);
                        operation.get(ModelDescriptionConstants.OP_ADDR).set(legacyAddress.toModelNode());
                        String operationName = operation.get(ModelDescriptionConstants.OP).asString();
                        context.addStep(operation, context.getRootResourceRegistration().getOperationHandler(legacyAddress, operationName), OperationContext.Stage.MODEL);
                    } else {
                        handler.execute(context, operation);
                    }
                }
            };
        }
    };

    static class LegacyAddOperationTransformation implements UnaryOperator<OperationStepHandler> {
        private final Predicate<ModelNode> legacy;

        <E extends Enum<E> & org.jboss.as.clustering.controller.Attribute> LegacyAddOperationTransformation(Class<E> attributeClass) {
            // If none of the specified attributes are defined, then this is a legacy operation
            this.legacy = operation -> {
                for (org.jboss.as.clustering.controller.Attribute attribute : EnumSet.allOf(attributeClass)) {
                    if (operation.hasDefined(attribute.getName())) return false;
                }
                return true;
            };
        }

        LegacyAddOperationTransformation(String... legacyProperties) {
            // If any of the specified properties are defined, then this is a legacy operation
            this.legacy = operation -> {
                if (!operation.hasDefined(Attribute.PROPERTIES.getName())) return false;
                for (String legacyProperty : legacyProperties) {
                    if (operation.get(Attribute.PROPERTIES.getName()).hasDefined(legacyProperty)) return true;
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
                    OperationStepHandler genericHandler = context.getResourceRegistration().getParent().getOperationHandler(PathAddress.pathAddress(ProtocolResourceDefinition.WILDCARD_PATH), ModelDescriptionConstants.ADD);
                    operation.get(ModelDescriptionConstants.OP_ADDR).set(context.getCurrentAddress().getParent().append(GenericProtocolResourceDefinition.pathElement(path.getValue())).toModelNode());
                    // Process this step first to preserve protocol order
                    context.addStep(operation, genericHandler, OperationContext.Stage.MODEL, true);
                } else {
                    handler.execute(context, operation);
                }
            };
        }
    }

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor).addCapabilities(Capability.class);
        }
    }

    ProtocolResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory serviceConfiguratorFactory, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(new Parameters(path, path.isWildcard() ? JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path) : JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, WILDCARD_PATH)).setOrderedChild(), new ResourceDescriptorConfigurator(configurator), serviceConfiguratorFactory, parentServiceConfiguratorFactory);
    }
}
