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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.NAME;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.VALUE;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} writing a single attribute. The required request parameter "name" represents the attribute name.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WriteAttributeHandler implements OperationStepHandler {

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(NAME, VALUE)
            .setRuntimeOnly()
            .build();

    public static final OperationStepHandler INSTANCE = new WriteAttributeHandler();

    private ParametersValidator nameValidator = new ParametersValidator();

    WriteAttributeHandler() {
        nameValidator.registerValidator(NAME.getName(), new StringLengthValidator(1));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME.getName()).asString();
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
        if (registry == null) {
            throw new OperationFailedException(ControllerMessages.MESSAGES.noSuchResourceType(PathAddress.pathAddress(operation.get(OP_ADDR))));
        }
        final AttributeAccess attributeAccess = registry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
        if (attributeAccess == null) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownAttribute(attributeName)));
        } else if (attributeAccess.getAccessType() != AttributeAccess.AccessType.READ_WRITE) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.attributeNotWritable(attributeName)));
        } else {

            // Authorize
            ModelNode currentValue;
            if (attributeAccess.getStorageType() == AttributeAccess.Storage.CONFIGURATION) {
                ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
                currentValue = model.has(attributeName) ? model.get(attributeName) : new ModelNode();
            } else {
                currentValue = new ModelNode();
            }
            AuthorizationResult authorizationResult = context.authorize(operation, attributeName, currentValue);
            if (authorizationResult.getDecision() == AuthorizationResult.Decision.DENY) {
                throw ControllerMessages.MESSAGES.unauthorized(operation.require(OP).asString(),
                        PathAddress.pathAddress(operation.get(OP_ADDR)),
                        authorizationResult.getExplanation());
            }

            if (attributeAccess.getStorageType() == AttributeAccess.Storage.CONFIGURATION
                    && !registry.isRuntimeOnly()) {
                // if the attribute is stored in the configuration, we can read its
                // old and new value from the resource's model before and after executing its write handler
                final ModelNode oldValue = currentValue.clone();
                OperationStepHandler writeHandler = attributeAccess.getWriteHandler();

                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(writeHandler.getClass());
                try {
                    writeHandler.execute(context, operation);
                    ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                    ModelNode newValue = model.has(attributeName) ? model.get(attributeName) : new ModelNode();
                    emitAttributeValueWrittenNotification(context, address, attributeName, oldValue, newValue);
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
            } else {
                assert attributeAccess.getStorageType() == AttributeAccess.Storage.RUNTIME;

                // if the attribute is a runtime attribute, its old and new values must
                // be read using the attribute's read handler and the write operation
                // must be sandwiched between the 2 calls to the read handler.
                // Each call to the read handlers will have their own results while
                // the call to the write handler will use this OSH context result.

                OperationContext.Stage currentStage = context.getCurrentStage();

                final ModelNode readAttributeOperation = Util.createOperation(READ_ATTRIBUTE_OPERATION, address);
                readAttributeOperation.get(NAME.getName()).set(attributeName);
                ReadAttributeHandler readAttributeHandler = new ReadAttributeHandler(null, null);

                // create 2 model nodes to store the result of the read-attribute operations
                // before and after writing the value
                final ModelNode oldValue = new ModelNode();
                final ModelNode newValue = new ModelNode();

                // 1st OSH is to read the old value
                context.addStep(oldValue, readAttributeOperation, readAttributeHandler, currentStage);

                // 2nd OSH is to write the value
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        doExecuteInternal(context, operation, attributeAccess);
                    }
                }, currentStage);

                // 3rd OSH is to read the new value
                context.addStep(newValue, readAttributeOperation, readAttributeHandler, currentStage);
                // 4th OSH is to emit the notification
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        // aggregate data from the 2 read-attribute operations
                        emitAttributeValueWrittenNotification(context, address, attributeName, oldValue.get(RESULT), newValue.get(RESULT));
                        context.stepCompleted();
                    }
                }, currentStage);

                context.stepCompleted();
            }
        }
    }

    private void doExecuteInternal(OperationContext context, ModelNode operation, AttributeAccess attributeAccess) throws OperationFailedException {
        OperationStepHandler writeHandler = attributeAccess.getWriteHandler();
        ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(writeHandler.getClass());
        try {
            writeHandler.execute(context, operation);
        } finally {
            SecurityActions.setThreadContextClassLoader(oldTccl);
        }
    }
    private void emitAttributeValueWrittenNotification(OperationContext context, PathAddress address, String attributeName, ModelNode oldValue, ModelNode newValue) {
        // only emit a notification if the value has been successfully changed
        if (oldValue.equals(newValue)) {
            return;
        }
        ModelNode data = new ModelNode();
        data.get(NAME.getName()).set(attributeName);
        data.get(GlobalNotifications.OLD_VALUE).set(oldValue);
        data.get(GlobalNotifications.NEW_VALUE).set(newValue);
        Notification notification = new Notification(ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION, address, ControllerMessages.MESSAGES.attributeValueWritten(attributeName, oldValue, newValue), data);
        context.emit(notification);
    }
}
