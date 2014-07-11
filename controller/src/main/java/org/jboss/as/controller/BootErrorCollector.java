/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.controller;

import org.jboss.as.controller.access.management.AuthorizedAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_ERROR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_ERRORS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MISSING_DEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class BootErrorCollector {

    private final ModelNode errors;
    private final OperationStepHandler listBootErrorsHandler;

    public BootErrorCollector() {
        errors = new ModelNode();
        errors.get(BOOT_ERRORS).setEmptyList();
        listBootErrorsHandler = new ListBootErrorsHandler(this);
    }
    void addFailureDescription(final ModelNode operation, final ModelNode failureDescription) {
        ModelNode error = new ModelNode();
        ModelNode failure = new ModelNode();
        // for security reasons failure.get(FAILED).set(operation.clone());
        ModelNode failed = failure.get(FAILED);
        failed.get(OP).set(operation.get(OP));
        failed.get(OP_ADDR).set(operation.get(OP_ADDR));
        if (failureDescription != null) {
            if (failureDescription.hasDefined(ControllerMessages.MESSAGES.failedServices())) {
                failure.get(FAILURES).add(failureDescription.get(ControllerMessages.MESSAGES.failedServices()));
            }
            if (failureDescription.hasDefined(ControllerMessages.MESSAGES.servicesMissingDependencies())) {
                failure.get(MISSING_DEPS).add(failureDescription.get(ControllerMessages.MESSAGES.servicesMissingDependencies()));
            }
            if (failureDescription.getType() == ModelType.STRING) {
                failure.get(FAILURES).add(failureDescription.asString());
            }
        }
        error.get(BOOT_ERROR).set(failure);
        errors.get(BOOT_ERRORS).add(error);
    }

    private ModelNode getErrors() {
        return errors.clone();
    }

    public OperationStepHandler getReadBootErrorsHandler() {
        return this.listBootErrorsHandler;
    }

    public static class ListBootErrorsHandler implements OperationStepHandler {

        public static final String OPERATION_NAME = "read-boot-errors";
        private final BootErrorCollector errors;

        private static final AttributeDefinition OP_DEFINITION = ObjectTypeAttributeDefinition.Builder.of(OP,
                SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.OPERATION_NAME, ModelType.STRING, false).build(),
                SimpleListAttributeDefinition.Builder.of(OP_ADDR,
                        SimpleAttributeDefinitionBuilder.create("address", ModelType.PROPERTY, false).build())
                        .build())
                .build();
        private static final AttributeDefinition BOOT_ERROR_DEFINITION = ObjectTypeAttributeDefinition.Builder.of(BOOT_ERROR,
                ObjectTypeAttributeDefinition.Builder.of(FAILED, OP_DEFINITION).build(),
                SimpleListAttributeDefinition.Builder.of(MISSING_DEPS,
                        SimpleAttributeDefinitionBuilder.create("missing-dep", ModelType.STRING, false).build())
                        .build(),
                SimpleListAttributeDefinition.Builder.of(FAILURES,
                        SimpleAttributeDefinitionBuilder.create("failure", ModelType.STRING, false).build())
                        .build())
                .build();
        private static final AttributeDefinition RETURN_DEFINITION = ObjectTypeAttributeDefinition.Builder.of(
                BOOT_ERRORS, BOOT_ERROR_DEFINITION).build();

        public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME,
                ControllerResolver.getResolver("errors")).setRuntimeOnly()
                .setReplyParameters(ObjectTypeAttributeDefinition.Builder.of(BOOT_ERRORS, RETURN_DEFINITION).build()).build();

        ListBootErrorsHandler(final BootErrorCollector errors) {
            this.errors = errors;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    ModelNode bootErrors = new ModelNode();
                    if(errors.getErrors().get(BOOT_ERRORS).isDefined()) {
                        for (ModelNode bootError : errors.getErrors().get(BOOT_ERRORS).asList()) {
                            secureOperationAddress(context, bootError);
                            bootErrors.get(BOOT_ERRORS).add(bootError);
                        }
                    }
                    context.getResult().set(errors.getErrors());
                    context.stepCompleted();
                }
            }, OperationContext.Stage.RUNTIME);
            context.stepCompleted();
        }

        private void secureOperationAddress(OperationContext context, ModelNode error) throws OperationFailedException {
            ModelNode bootError = error.get(BOOT_ERROR);
            if (bootError.hasDefined(FAILED)) {
                ModelNode failedOperation = bootError.get(FAILED);
                ModelNode address = failedOperation.get(OP_ADDR);
                ModelNode fakeOperation = new ModelNode();
                fakeOperation.get(ModelDescriptionConstants.OP).set(READ_RESOURCE_OPERATION);
                fakeOperation.get(ModelDescriptionConstants.OP_ADDR).set(address);
                AuthorizedAddress authorizedAddress = AuthorizedAddress.authorizeAddress(context, fakeOperation);
                if(authorizedAddress.isElided()) {
                    failedOperation.get(OP_ADDR).set(authorizedAddress.getAddress());
                }
            }
        }
    }
}
