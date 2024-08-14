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
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.dmr.ModelNode;
import org.jgroups.Global;
import org.jgroups.protocols.FD_ALL2;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Registers a resource definition for a legacy protocol.
 * @author Paul Ferraro
 */
public class LegacyProtocolResourceDefinitionRegistrar extends ProtocolChildResourceDefinitionRegistrar {
    enum Protocol implements ResourceRegistration {
        FD(FD_ALL2.class, JGroupsSubsystemModel.VERSION_10_0_0),
        ;
        private final PathElement path;
        private final PathElement targetPath;
        private final JGroupsSubsystemModel deprecation;

        Protocol(Class<? extends org.jgroups.stack.Protocol> targetProtocol, JGroupsSubsystemModel deprecation) {
            this(null, targetProtocol, deprecation);
        }

        Protocol(String name, Class<? extends org.jgroups.stack.Protocol> targetProtocol, JGroupsSubsystemModel deprecation) {
            this.path = StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement((name != null) ? name : this.name());
            this.targetPath = StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(targetProtocol.getName().substring(Global.PREFIX.length()));
            this.deprecation = deprecation;
        }

        @Override
        public PathElement getPathElement() {
            return this.path;
        }

        PathElement getTargetPathElement() {
            return this.targetPath;
        }

        JGroupsSubsystemModel getDeprecation() {
            return this.deprecation;
        }
    }

    private final PathElement targetPath;

    LegacyProtocolResourceDefinitionRegistrar(Protocol protocol) {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return protocol;
            }

            @Override
            public ResourceDescriptionResolver getResourceDescriptionResolver() {
                return JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(protocol.getPathElement(), StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement());
            }

            @Override
            public JGroupsSubsystemModel getDeprecation() {
                return protocol.getDeprecation();
            }
        });
        this.targetPath = protocol.getTargetPathElement();
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                // Redirect operations for legacy protocol to its replacement address
                .withOperationTransformation(Set.of(ModelDescriptionConstants.ADD, ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), new UnaryOperator<>() {
                    @Override
                    public OperationStepHandler apply(OperationStepHandler handler) {
                        return new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) {
                                PathAddress address = context.getCurrentAddress();
                                JGroupsLogger.ROOT_LOGGER.legacyProtocol(address.getLastElement().getValue(), LegacyProtocolResourceDefinitionRegistrar.this.targetPath.getValue());
                                PathAddress targetAddress = address.getParent().append(LegacyProtocolResourceDefinitionRegistrar.this.targetPath);
                                operation.get(ModelDescriptionConstants.OP_ADDR).set(targetAddress.toModelNode());
                                PathAddress targetRegistrationAddress = address.getParent().append(StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement());
                                String operationName = operation.get(ModelDescriptionConstants.OP).asString();
                                context.addStep(operation, context.getRootResourceRegistration().getOperationHandler(targetRegistrationAddress, operationName), OperationContext.Stage.MODEL, true);
                            }
                        };
                    }
                });
    }
}
