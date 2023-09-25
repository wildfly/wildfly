/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.shallow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class TranslatedOperationHandler implements OperationStepHandler {

    private final OperationAddressConverter converter;

    public TranslatedOperationHandler(OperationAddressConverter converter) {
        this.converter = converter;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress targetAddress = converter.convert(context, operation);
        ModelNode op = operation.clone();
        op.get(OP_ADDR).set(targetAddress.toModelNode());
        String operationName = op.require(OP).asString();
        OperationStepHandler operationHandler = context.getRootResourceRegistration().getOperationHandler(targetAddress, operationName);
        context.addStep(op, operationHandler, context.getCurrentStage(), true);
    }
}
