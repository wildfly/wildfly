/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.RestartParentResourceAddStepHandler;
import org.jboss.as.clustering.controller.RestartParentResourceRemoveStepHandler;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.LegacyPropertyAddOperationTransformer;
import org.jboss.as.clustering.controller.transform.LegacyPropertyResourceTransformer;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * Resource definition for a protocol of a jgroups protocol stack.
 * @author Paul Ferraro
 */
public class StackProtocolResourceDefinition extends ProtocolResourceDefinition {

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        ProtocolResourceDefinition.addTransformations(version, builder);

        if (JGroupsModel.VERSION_4_1_0.requiresTransformation(version)) {
            // See WFLY-6782, add-index parameter was missing from add operation definition
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, ModelDescriptionConstants.ADD_INDEX);
        }

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Translate /subsystem=jgroups/stack=*/protocol=*:add() -> /subsystem=jgroups/stack=*:add-protocol()
            OperationTransformer addTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    PathAddress stackAddress = address.subAddress(0, address.size() - 1);
                    ModelNode addProtocolOp = operation.clone();
                    addProtocolOp.get(ModelDescriptionConstants.OP_ADDR).set(stackAddress.toModelNode());
                    addProtocolOp.get(ModelDescriptionConstants.OP).set("add-protocol");

                    addProtocolOp = new LegacyPropertyAddOperationTransformer().transformOperation(addProtocolOp);

                    return addProtocolOp;
                }
            };
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD).setCustomOperationTransformer(new SimpleOperationTransformer(addTransformer)).inheritResourceAttributeDefinitions();

            // Translate /subsystem=jgroups/stack=*/protocol=*:remove() -> /subsystem=jgroups/stack=*:remove-protocol()
            OperationTransformer removeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    String protocol = address.getLastElement().getValue();
                    PathAddress stackAddress = address.subAddress(0, address.size() - 1);
                    ModelNode legacyOperation = Util.createOperation("remove-protocol", stackAddress);
                    legacyOperation.get(ProtocolResourceDefinition.DeprecatedAttribute.TYPE.getName()).set(protocol);
                    return legacyOperation;
                }
            };
            builder.addOperationTransformationOverride(ModelDescriptionConstants.REMOVE).setCustomOperationTransformer(new SimpleOperationTransformer(removeTransformer));

            builder.setCustomResourceTransformer(new LegacyPropertyResourceTransformer());
        }
    }

    StackProtocolResourceDefinition(ResourceServiceBuilderFactory<ChannelFactory> stackBuilderFactory) {
        super(stackBuilderFactory);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addExtraParameters(DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(address -> new ProtocolConfigurationBuilder(address));
        new RestartParentResourceAddStepHandler<>(this.parentBuilderFactory, descriptor, handler).register(registration);
        new RestartParentResourceRemoveStepHandler<>(this.parentBuilderFactory, descriptor, handler).register(registration);

        for (DeprecatedAttribute attribute : DeprecatedAttribute.values()) {
            registration.registerReadOnlyAttribute(attribute.getDefinition(), null);
        }

        super.register(registration);
    }
}
