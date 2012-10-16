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
package org.jboss.as.host.controller.model.jvm;

import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.descriptions.HostRootDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public final class JVMEnvironmentVariableAddHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "add-item-to-environment-variables-list";
    static final JVMEnvironmentVariableAddHandler INSTANCE = new JVMEnvironmentVariableAddHandler();

    static final SimpleAttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.NAME, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .build();
    private static final SimpleAttributeDefinition VALUE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.VALUE, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .build();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, HostRootDescription.getResourceDescriptionResolver("jvm"))
        .addParameter(NAME)
        .addParameter(VALUE)
        .build();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();

        final String name = NAME.validateOperation(operation).asString();
        final String value = VALUE.validateOperation(operation).asString();
        ModelNode variables = model.get(JvmAttributes.JVM_ENV_VARIABLES);
        if (variables.isDefined()) {
            for (ModelNode varNode : variables.asList()) {
                if (varNode.asProperty().getName().equals(name)) {
                    throw MESSAGES.envVariableAlreadyExists(varNode.asProperty().getName());
                }
            }
        }
        model.get(JvmAttributes.JVM_ENV_VARIABLES).add(name, value);

        context.stepCompleted();
    }
}
