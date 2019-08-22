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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Set;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.ReadAttributeHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class TranslatedReadAttributeHandler implements OperationStepHandler {

    private final OperationAddressConverter converter;
    private final IgnoredAttributeProvider provider;

    public TranslatedReadAttributeHandler(OperationAddressConverter converter, IgnoredAttributeProvider provider) {
        this.converter = converter;
        this.provider = provider;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress targetAddress = converter.convert(context, operation);
        Set<String> ignoredAttributes = provider.getIgnoredAttributes(context, operation);
        ModelNode op = operation.clone();
        String attribute = op.require(ModelDescriptionConstants.NAME).asString();
        if (ignoredAttributes.contains(attribute)) {
            return;
        }
        op.get(OP_ADDR).set(targetAddress.toModelNode());
        OperationStepHandler readAttributeHandler = context.getRootResourceRegistration().getAttributeAccess(targetAddress, attribute).getReadHandler();
        if (readAttributeHandler == null) {
            readAttributeHandler = ReadAttributeHandler.INSTANCE;
        }
        context.addStep(op, readAttributeHandler, context.getCurrentStage(), true);
    }
}
