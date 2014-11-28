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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED_SERVICES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_TIMESTAMP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MISSING_TRANSITIVE_DEPENDENCY_PROBLEMS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.POSSIBLE_CAUSES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICES_MISSING_DEPENDENCIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICES_MISSING_TRANSITIVE_DEPENDENCIES;

import org.jboss.as.controller.access.management.AuthorizedAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class BootErrorCollector {

    private final ModelNode errors;
    private final OperationStepHandler listBootErrorsHandler;

    public BootErrorCollector() {
        errors = new ModelNode();
        errors.setEmptyList();
        listBootErrorsHandler = new ListBootErrorsHandler(this);
    }

    void addFailureDescription(final ModelNode operation, final ModelNode failureDescription) {
        assert operation != null;
        assert failureDescription != null;

        ModelNode error = new ModelNode();
        // for security reasons failure.get(FAILED).set(operation.clone());
        ModelNode failedOperation = error.get(FAILED_OPERATION);
        failedOperation.get(OP).set(operation.get(OP));
        error.get(FAILURE_TIMESTAMP).set(System.currentTimeMillis());
        ModelNode opAddr = operation.get(OP_ADDR);
        if (!opAddr.isDefined()) {
            opAddr.setEmptyList();
        }
        failedOperation.get(OP_ADDR).set(opAddr);

        error.get(FAILURE_DESCRIPTION).set(failureDescription.asString());
        ModelNode report = ServiceVerificationHandler.extractFailedServicesDescription(failureDescription);
        if (report != null) {
            error.get(FAILED_SERVICES).set(report);
        }
        report = ServiceVerificationHandler.extractMissingServicesDescription(failureDescription);
        if (report != null) {
            error.get(SERVICES_MISSING_DEPENDENCIES).set(report);
        }
        report = ServiceVerificationHandler.extractTransitiveDependencyProblemDescription(failureDescription);
        if (report != null) {
            error.get(MISSING_TRANSITIVE_DEPENDENCY_PROBLEMS).set(report);
        }

        synchronized (errors) {
            errors.add(error);
        }
    }

    private ModelNode getErrors() {
        synchronized (errors) {
            return errors.clone();
        }
    }

    public OperationStepHandler getReadBootErrorsHandler() {
        return this.listBootErrorsHandler;
    }

    public static class ListBootErrorsHandler implements OperationStepHandler {

        public static final String OPERATION_NAME = "read-boot-errors";
        private final BootErrorCollector errors;

        private static final AttributeDefinition OP_DEFINITION = ObjectTypeAttributeDefinition.Builder.of(FAILED_OPERATION,
                    SimpleAttributeDefinitionBuilder.create(OP, ModelType.STRING, false).build(),
                    SimpleAttributeDefinitionBuilder.create(FAILURE_TIMESTAMP, ModelType.LONG, false).build(),
                    SimpleListAttributeDefinition.Builder.of(OP_ADDR,
                            SimpleAttributeDefinitionBuilder.create("element", ModelType.PROPERTY, false).build())
                            .build())
                .setAllowNull(false)
                .build();

        private static final AttributeDefinition FAILURE_MESSAGE = SimpleAttributeDefinitionBuilder.create(FAILURE_DESCRIPTION, ModelType.STRING, false).build();

        private static final AttributeDefinition FAILED_SVC_AD = SimpleListAttributeDefinition.Builder.of(FAILED_SERVICES,
                SimpleAttributeDefinitionBuilder.create("element", ModelType.STRING, false).build())
                .setAllowNull(true)
                .build();

        private static final AttributeDefinition MISSING_DEPS_AD = SimpleListAttributeDefinition.Builder.of(SERVICES_MISSING_DEPENDENCIES,
                SimpleAttributeDefinitionBuilder.create("element", ModelType.STRING, false).build())
                .setAllowNull(true)
                .build();

        private static final AttributeDefinition AFFECTED_AD = SimpleListAttributeDefinition.Builder.of(SERVICES_MISSING_TRANSITIVE_DEPENDENCIES,
                    SimpleAttributeDefinitionBuilder.create("element", ModelType.STRING, false).build())
                .build();

        private static final AttributeDefinition CAUSE_AD = SimpleListAttributeDefinition.Builder.of(POSSIBLE_CAUSES,
                SimpleAttributeDefinitionBuilder.create("element", ModelType.STRING, false).build())
                .build();

        private static final AttributeDefinition TRANSITIVE_AD = ObjectTypeAttributeDefinition.Builder.of(MISSING_TRANSITIVE_DEPENDENCY_PROBLEMS,
                    AFFECTED_AD, CAUSE_AD)
                .setAllowNull(true)
                .build();

        public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME,
                ControllerResolver.getResolver("errors"))
                .setReadOnly()
                .setRuntimeOnly()
                .setReplyType(ModelType.LIST)
                .setReplyParameters(OP_DEFINITION, FAILURE_MESSAGE, FAILED_SVC_AD, MISSING_DEPS_AD, TRANSITIVE_AD).build();

        ListBootErrorsHandler(final BootErrorCollector errors) {
            this.errors = errors;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    ModelNode bootErrors = new ModelNode().setEmptyList();
                    ModelNode errorsNode = errors.getErrors();
                    for (ModelNode bootError : errorsNode.asList()) {
                        secureOperationAddress(context, bootError);
                        bootErrors.add(bootError);
                    }
                    context.getResult().set(bootErrors);
                    context.stepCompleted();
                }
            }, OperationContext.Stage.RUNTIME);
            context.stepCompleted();
        }

        private void secureOperationAddress(OperationContext context, ModelNode bootError) throws OperationFailedException {
            if (bootError.hasDefined(FAILED_OPERATION)) {
                ModelNode failedOperation = bootError.get(FAILED_OPERATION);
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
