/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.dmr.ModelNode;

import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class LogStoreParticipantRefreshHandler implements OperationStepHandler {

    static final LogStoreParticipantRefreshHandler INSTANCE = new LogStoreParticipantRefreshHandler();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        MBeanServer mbs = TransactionExtension.getMBeanServer(context);
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        try {
            final ObjectName on = LogStoreResource.getObjectName(resource);
            final ModelNode model = resource.getModel().clone();

            AttributeList attributes = mbs.getAttributes(on, LogStoreConstants.PARTICIPANT_JMX_NAMES);

            for (javax.management.Attribute attribute : attributes.asList()) {
                String modelName = LogStoreConstants.jmxNameToModelName(LogStoreConstants.MODEL_TO_JMX_PARTICIPANT_NAMES, attribute.getName());

                if (modelName != null) {
                    ModelNode aNode = model.get(modelName);
                    Object value = attribute.getValue();

                    if (aNode != null)
                        aNode.set(value == null ? "" : value.toString());
                }
            }
            // Replace the model
            resource.writeModel(model);
        } catch (Exception e) {
            throw TransactionLogger.ROOT_LOGGER.jmxError(e.getMessage());
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}
