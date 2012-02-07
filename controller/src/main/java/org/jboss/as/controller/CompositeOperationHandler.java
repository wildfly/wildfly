/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the "composite" operation; i.e. one that includes one or more child operations
 * as steps.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public final class CompositeOperationHandler implements OperationStepHandler, DescriptionProvider {
    public static final CompositeOperationHandler INSTANCE = new CompositeOperationHandler();
    public static final String NAME = ModelDescriptionConstants.COMPOSITE;

    private CompositeOperationHandler() {
    }

    public void execute(final OperationContext context, final ModelNode operation) {
        ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
        final List<ModelNode> list = operation.get(STEPS).asList();
        ModelNode responseMap = context.getResult().setEmptyObject();
        Map<String, OperationStepHandler> stepHandlerMap = new HashMap<String, OperationStepHandler>();
        final int size = list.size();
        // Validate all needed handlers are available.
        for (int i = 0; i < size; i++) {
            String stepName = "step-" + (i+1);
            // This makes the result steps appear in the correct order
            responseMap.get(stepName);
            final ModelNode subOperation = list.get(i);
            PathAddress stepAddress = PathAddress.pathAddress(subOperation.get(OP_ADDR));
            String stepOpName = subOperation.require(OP).asString();
            OperationStepHandler stepHandler = registry.getOperationHandler(stepAddress, stepOpName);
            if (stepHandler == null) {
                context.getFailureDescription().set(MESSAGES.noHandler(stepOpName, stepAddress));
                context.completeStep();
                return;
            }
            stepHandlerMap.put(stepName, stepHandler);
        }

        for (int i = size - 1; i >= 0; i --) {
            final ModelNode subOperation = list.get(i);
            String stepName = "step-" + (i+1);
            context.addStep(responseMap.get(stepName).setEmptyObject(), subOperation, stepHandlerMap.get(stepName), OperationContext.Stage.IMMEDIATE);
        }

        if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
            final ModelNode failureMsg = new ModelNode();
            for (int i = 0; i < size; i++) {
                String stepName = "step-" + (i+1);
                ModelNode stepResponse = responseMap.get(stepName);
                if (stepResponse.hasDefined(FAILURE_DESCRIPTION)) {
                    failureMsg.get(MESSAGES.compositeOperationFailed(), MESSAGES.operation(stepName)).set(stepResponse.get(FAILURE_DESCRIPTION));
                }
            }
            if (!failureMsg.isDefined()) {
                failureMsg.set(MESSAGES.compositeOperationRolledBack());
            }
            context.getFailureDescription().set(failureMsg);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        //Since this instance should have EntryType.PRIVATE, there is no need for a description
        //return new ModelNode();
        return CommonDescriptions.getCompositeOperation(locale);
    }
}
