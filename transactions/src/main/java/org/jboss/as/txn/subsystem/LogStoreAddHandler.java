/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
class LogStoreAddHandler implements OperationStepHandler {
    private LogStoreResource resource = null;

    LogStoreAddHandler(LogStoreResource resource) {
        this.resource = resource;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        // Add the log store resource
        final ModelNode model = resource.getModel();

        LogStoreConstants.LOG_STORE_TYPE.validateAndSet(operation, model);

        context.addResource(PathAddress.EMPTY_ADDRESS, resource);
    }
}
