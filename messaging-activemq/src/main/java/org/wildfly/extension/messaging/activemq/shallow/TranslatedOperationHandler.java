/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
