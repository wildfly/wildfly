/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Registers a resource definition for a legacy protocol.
 * @author Paul Ferraro
 */
public class LegacyProtocolResourceDefinitionRegistrar extends ProtocolChildResourceDefinitionRegistrar {

    LegacyProtocolResourceDefinitionRegistrar(LegacyProtocolResourceDescription description) {
        super(new ProtocolChildResourceDescriptorConfigurator() {
            @Override
            public ProtocolResourceDescription getResourceDescription() {
                return description;
            }

            @Override
            public ResourceDescriptionResolver getResourceDescriptionResolver() {
                return JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(description.getPathElement(), ProtocolResourceDescription.INSTANCE.getPathElement());
            }

            @Override
            public JGroupsSubsystemModel getDeprecation() {
                return description.getDeprecation();
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return ProtocolChildResourceDescriptorConfigurator.super.apply(builder)
                        .withOperationTransformation(Set.of(ModelDescriptionConstants.ADD, ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), new UnaryOperator<>() {
                            @Override
                            public OperationStepHandler apply(OperationStepHandler handler) {
                                return new OperationStepHandler() {
                                    @Override
                                    public void execute(OperationContext context, ModelNode operation) {
                                        PathAddress address = context.getCurrentAddress();
                                        JGroupsLogger.ROOT_LOGGER.legacyProtocol(address.getLastElement().getValue(), description.getTargetPathElement().getValue());
                                        PathAddress targetAddress = address.getParent().append(description.getTargetPathElement());
                                        operation.get(ModelDescriptionConstants.OP_ADDR).set(targetAddress.toModelNode());
                                        PathAddress targetRegistrationAddress = address.getParent().append(ProtocolResourceDescription.INSTANCE.getPathElement());
                                        String operationName = operation.get(ModelDescriptionConstants.OP).asString();
                                        context.addStep(operation, context.getRootResourceRegistration().getOperationHandler(targetRegistrationAddress, operationName), OperationContext.Stage.MODEL, true);
                                    }
                                };
                            }
                        });
            }
        });
    }
}
