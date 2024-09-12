/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Resource definition for legacy protocols.
 * @author Paul Ferraro
 */
public class LegacyProtocolResourceDefinition<P extends Protocol> extends ProtocolResourceDefinition<P> {

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<OperationStepHandler> operationTransformation;
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(String targetName, UnaryOperator<ResourceDescriptor> configurator) {
            this.operationTransformation = new OperationTransformation(targetName);
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor).setAddOperationTransformation(this.operationTransformation).setOperationTransformation(this.operationTransformation);
        }
    }

    private static class OperationTransformation implements UnaryOperator<OperationStepHandler>, OperationStepHandler {
        private final String targetName;

        OperationTransformation(String targetName) {
            this.targetName = targetName;
        }

        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return this;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) {
            PathAddress address = context.getCurrentAddress();
            JGroupsLogger.ROOT_LOGGER.legacyProtocol(address.getLastElement().getValue(), this.targetName);
            PathAddress targetAddress = address.getParent().append(pathElement(this.targetName));
            operation.get(ModelDescriptionConstants.OP_ADDR).set(targetAddress.toModelNode());
            PathAddress targetRegistrationAddress = address.getParent().append(ProtocolResourceDefinition.WILDCARD_PATH);
            String operationName = operation.get(ModelDescriptionConstants.OP).asString();
            context.addStep(operation, context.getRootResourceRegistration().getOperationHandler(targetRegistrationAddress, operationName), OperationContext.Stage.MODEL, true);
        }
    }

    LegacyProtocolResourceDefinition(String name, String targetName, JGroupsSubsystemModel deprecation, UnaryOperator<ResourceDescriptor> configurator) {
        super(pathElement(name), new ResourceDescriptorConfigurator(targetName, configurator), null);
        this.setDeprecated(deprecation.getVersion());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return ResourceServiceInstaller.combine();
    }
}
