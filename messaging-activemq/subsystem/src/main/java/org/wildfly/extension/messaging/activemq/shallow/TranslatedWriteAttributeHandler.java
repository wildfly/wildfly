/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.shallow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class TranslatedWriteAttributeHandler implements OperationStepHandler {

    private static final Logger LOG = Logger.getLogger(TranslatedWriteAttributeHandler.class);

    private final ShallowResourceDefinition shallowResource;

    public TranslatedWriteAttributeHandler(ShallowResourceDefinition shallowResource) {
        this.shallowResource = shallowResource;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress targetAddress = shallowResource.convert(context, operation);
        ModelNode op = operation.clone();
        op.get(OP_ADDR).set(targetAddress.toModelNode());
        String attributeName = op.get(ModelDescriptionConstants.NAME).asString();
        if (!shallowResource.getIgnoredAttributes(context, op).contains(attributeName)) {
            shallowResource.validateOperation(context, targetAddress, op);
            OperationStepHandler writeAttributeHandler = context.getRootResourceRegistration()
                    .getAttributeAccess(targetAddress, attributeName).getWriteHandler();
            context.addStep(op, writeAttributeHandler, context.getCurrentStage(), true);
        } else {
            LOG.debugf("Ignoring write operation on resource %s, attribute %s. The attribute is ignored.",
                    targetAddress.toString(), attributeName);
        }
    }
}