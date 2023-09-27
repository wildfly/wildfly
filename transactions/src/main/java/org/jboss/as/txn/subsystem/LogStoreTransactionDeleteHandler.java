/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Operation handler for removing transaction logs from the TM and from the subsystem model
 */
public class LogStoreTransactionDeleteHandler implements OperationStepHandler {
    // Generic failure reason if we cannot determine a more specific cause
    static final String LOG_DELETE_FAILURE_MESSAGE = "Unable to remove transaction log";

    private LogStoreResource logStoreResource = null;

    LogStoreTransactionDeleteHandler(LogStoreResource resource) {
        this.logStoreResource = resource;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        MBeanServer mbs = TransactionExtension.getMBeanServer(context);
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);

        try {
            final ObjectName on = LogStoreResource.getObjectName(resource);

            //  Invoke operation
            Object res = mbs.invoke(on, "remove", null, null);

            try {
                // validate that the MBean was removed:
                mbs.getObjectInstance(on);

                String reason = res != null ? res.toString() : LOG_DELETE_FAILURE_MESSAGE;

                throw new OperationFailedException(reason);
            } catch (InstanceNotFoundException e) {
                // success, the MBean was deleted
                final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
                final PathElement element = address.getLastElement();

                logStoreResource.removeChild(element);
            }
        } catch (OperationFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage());
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);

    }
}
