/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class ProtocolResourceDefinition<P extends Protocol> extends AbstractProtocolResourceDefinition<P, ProtocolConfiguration<P>> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("protocol", name);
    }

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ProtocolConfiguration.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

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

    ProtocolResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(new Parameters(path, path.isWildcard() ? JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path) : JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, WILDCARD_PATH)).setOrderedChild(), CAPABILITY, configurator, parentServiceConfigurator);
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<P>, ProtocolConfiguration<P>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return Map.entry(UnaryOperator.identity(), Functions.discardingConsumer());
    }
}
