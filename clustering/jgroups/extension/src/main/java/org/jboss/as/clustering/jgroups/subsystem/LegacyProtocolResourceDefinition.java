/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Resource definition for legacy protocols.
 * @author Paul Ferraro
 */
public class LegacyProtocolResourceDefinition extends ProtocolResourceDefinition {

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
            Operations.setPathAddress(operation, targetAddress);
            PathAddress targetRegistrationAddress = address.getParent().append(ProtocolResourceDefinition.WILDCARD_PATH);
            String operationName = Operations.getName(operation);
            context.addStep(operation, context.getRootResourceRegistration().getOperationHandler(targetRegistrationAddress, operationName), OperationContext.Stage.MODEL, true);
        }
    }

    LegacyProtocolResourceDefinition(String name, String targetName, JGroupsModel deprecation, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(pathElement(name), new ResourceDescriptorConfigurator(targetName, configurator), null, parentServiceConfiguratorFactory);
        this.setDeprecated(deprecation.getVersion());
    }
}
