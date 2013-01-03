/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JMXSubsystemRootResource extends SimpleResourceDefinition {

    private static final PathElement RESOLVED_PATH = PathElement.pathElement(CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED);

    SimpleAttributeDefinition SHOW_MODEL_ALIAS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SHOW_MODEL, ModelType.BOOLEAN, true)
            .addFlag(AttributeAccess.Flag.ALIAS)
            .build();

    JMXSubsystemRootResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME),
                JMXExtension.getResourceDescriptionResolver(JMXExtension.SUBSYSTEM_NAME),
                JMXSubsystemAdd.INSTANCE,
                JMXSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(SHOW_MODEL_ALIAS, ShowModelAliasReadHandler.INSTANCE, ShowModelAliasWriteHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(ExposeModelResourceResolved.INSTANCE);
        resourceRegistration.registerSubModel(ExposeModelResourceExpression.INSTANCE);
        resourceRegistration.registerSubModel(RemotingConnectorResource.INSTANCE);
    }


    private static class ShowModelAliasWriteHandler implements OperationStepHandler {
        static final ShowModelAliasWriteHandler INSTANCE = new ShowModelAliasWriteHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final boolean value = operation.get(ModelDescriptionConstants.VALUE).asBoolean(false);
            boolean hasResource = context.readResource(PathAddress.EMPTY_ADDRESS).hasChild(RESOLVED_PATH);
            if (value) {
                if (!hasResource) {
                    OperationStepHandler handler = context.getResourceRegistration().getOperationEntry(PathAddress.pathAddress(RESOLVED_PATH), ADD).getOperationHandler();
                    ModelNode addOp = new ModelNode();
                    addOp.get(OP).set(ADD);
                    addOp.get(OP_ADDR).set(PathAddress.pathAddress(operation.get(OP_ADDR)).append(RESOLVED_PATH).toModelNode());
                    context.addStep(addOp, handler, Stage.IMMEDIATE);
                }
            } else {
                if (hasResource) {
                    OperationStepHandler handler = context.getResourceRegistration().getOperationEntry(PathAddress.pathAddress(RESOLVED_PATH), REMOVE).getOperationHandler();
                    ModelNode addOp = new ModelNode();
                    addOp.get(OP).set(REMOVE);
                    addOp.get(OP_ADDR).set(PathAddress.pathAddress(operation.get(OP_ADDR)).append(RESOLVED_PATH).toModelNode());
                    context.addStep(addOp, handler, Stage.IMMEDIATE);
                }
            }
            context.stepCompleted();
        }
    }

    private static class ShowModelAliasReadHandler implements OperationStepHandler {
        static final ShowModelAliasReadHandler INSTANCE = new ShowModelAliasReadHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            context.getResult().set(resource.hasChild(PathElement.pathElement(CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED)));
            context.stepCompleted();
        }

    }

}
