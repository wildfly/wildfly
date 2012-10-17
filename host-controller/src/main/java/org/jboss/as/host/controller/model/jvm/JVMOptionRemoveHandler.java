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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

final class JVMOptionRemoveHandler implements OperationStepHandler {

    static final String OPERATION_NAME = "remove-jvm-option";
    static final JVMOptionRemoveHandler INSTANCE = new JVMOptionRemoveHandler();

    static final SimpleAttributeDefinition JVM_OPTION = SimpleAttributeDefinitionBuilder.create(JvmAttributes.JVM_OPTION, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .build();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, HostResolver.getResolver("jvm"))
        .addParameter(JVM_OPTION)
        .build();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();

        final ModelNode option = JVM_OPTION.validateOperation(operation);
        if (model.hasDefined(JvmAttributes.JVM_OPTIONS)) {
            final ModelNode values = model.get(JvmAttributes.JVM_OPTIONS).clone();
            model.get(JvmAttributes.JVM_OPTIONS).setEmptyList();

            for (ModelNode value : values.asList()) {
                if (!value.equals(option)) {
                    model.get(JvmAttributes.JVM_OPTIONS).add(value);
                }
            }
        }

        context.stepCompleted();
    }
}
