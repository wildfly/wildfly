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

import javax.management.MBeanServer;
import javax.management.ObjectName;

abstract class LogStoreParticipantOperationHandler implements OperationStepHandler {

    private String operationName;

    public LogStoreParticipantOperationHandler(String operationName) {
        this.operationName = operationName;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        MBeanServer mbs = TransactionExtension.getMBeanServer(context);
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        try {
            // Get the internal object name
            final ObjectName on = LogStoreResource.getObjectName(resource);

            //  Invoke the MBean operation
            mbs.invoke(on, operationName, null, null);

        } catch (Exception e) {
            throw TransactionLogger.ROOT_LOGGER.jmxError(e.getMessage());
        }

        // refresh the attributes of this participant (the status attribute should have changed to PREPARED
        refreshParticipant(context);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    abstract void refreshParticipant(OperationContext context);
}
