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

import static org.jboss.as.host.controller.model.jvm.JVMEnvironmentVariableAddHandler.NAME;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

public final class JVMEnvironmentVariableRemoveHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "remove-item-from-environment-variables-list";
    static final JVMEnvironmentVariableRemoveHandler INSTANCE = new JVMEnvironmentVariableRemoveHandler();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, HostResolver.getResolver("jvm"))
        .addParameter(NAME)
        .build();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();

        final String name = NAME.validateOperation(operation).asString();
        ModelNode variables = model.get(JvmAttributes.JVM_ENV_VARIABLES);
        if (variables.isDefined()) {
            final ModelNode values = variables.clone();
            model.get(JvmAttributes.JVM_ENV_VARIABLES).setEmptyList();

            for (Property value : values.asPropertyList()) {
                if (!value.getName().equals(name)) {
                    model.get(JvmAttributes.JVM_ENV_VARIABLES).add(value.getName(), value.getValue());
                }
            }
        }

        context.stepCompleted();
    }
}
