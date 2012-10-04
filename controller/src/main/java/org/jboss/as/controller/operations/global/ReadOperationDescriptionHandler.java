/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_REQUIRED;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.NAME;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.LOCALE;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} returning the type description of a single operation description.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ReadOperationDescriptionHandler implements OperationStepHandler {

    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_OPERATION_DESCRIPTION_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(NAME, LOCALE)
            .setReplyType(ModelType.OBJECT)
            .setReadOnly()
            .setRuntimeOnly()
            .build();

    static final OperationStepHandler INSTANCE = new ReadOperationDescriptionHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        String operationName = NAME.resolveModelAttribute(context, operation).asString();

        final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
        OperationEntry operationEntry = registry.getOperationEntry(PathAddress.EMPTY_ADDRESS, operationName);
        if (operationEntry == null || (context.getProcessType() == ProcessType.DOMAIN_SERVER && !operationEntry.getFlags().contains(OperationEntry.Flag.RUNTIME_ONLY))) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.operationNotRegistered(operationName,
                    PathAddress.pathAddress(operation.require(OP_ADDR)))));
        } else {
            final ModelNode result = operationEntry.getDescriptionProvider().getModelDescription(GlobalOperationHandlers.getLocale(context, operation));
            Set<OperationEntry.Flag> flags = operationEntry.getFlags();
            boolean readOnly = flags.contains(OperationEntry.Flag.READ_ONLY);
            result.get(READ_ONLY).set(readOnly);
            if (!readOnly) {
                if (flags.contains(OperationEntry.Flag.RESTART_ALL_SERVICES)) {
                    result.get(RESTART_REQUIRED).set("all-services");
                } else if (flags.contains(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)) {
                    result.get(RESTART_REQUIRED).set("resource-services");
                } else if (flags.contains(OperationEntry.Flag.RESTART_JVM)) {
                    result.get(RESTART_REQUIRED).set("jvm");
                }
            }

            context.getResult().set(result);
        }
        context.stepCompleted();
    }
}
