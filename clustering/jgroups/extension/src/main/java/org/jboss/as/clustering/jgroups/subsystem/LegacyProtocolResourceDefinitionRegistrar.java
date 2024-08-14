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
import org.jboss.as.controller.PathElement;
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

    LegacyProtocolResourceDefinitionRegistrar(String name, String targetName, JGroupsSubsystemModel deprecation) {
        super(new ProtocolChildResourceRegistration() {
            @Override
            public PathElement getPathElement() {
                return AbstractProtocolResourceDefinitionRegistrar.pathElement(name);
            }

            @Override
            public ResourceDescriptionResolver getResourceDescriptionResolver() {
                return JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.getPathElement(), ProtocolResourceDefinitionRegistrar.WILDCARD_PATH);
            }

            @Override
            public JGroupsSubsystemModel getDeprecation() {
                return deprecation;
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return ProtocolChildResourceRegistration.super.apply(builder)
                        .withOperationTransformation(Set.of(ModelDescriptionConstants.ADD, ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), new UnaryOperator<>() {
                            @Override
                            public OperationStepHandler apply(OperationStepHandler handler) {
                                return new OperationStepHandler() {
                                    @Override
                                    public void execute(OperationContext context, ModelNode operation) {
                                        PathAddress address = context.getCurrentAddress();
                                        JGroupsLogger.ROOT_LOGGER.legacyProtocol(address.getLastElement().getValue(), targetName);
                                        PathAddress targetAddress = address.getParent().append(AbstractProtocolResourceDefinitionRegistrar.pathElement(targetName));
                                        operation.get(ModelDescriptionConstants.OP_ADDR).set(targetAddress.toModelNode());
                                        PathAddress targetRegistrationAddress = address.getParent().append(ProtocolResourceDefinitionRegistrar.WILDCARD_PATH);
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
