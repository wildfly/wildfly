/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
