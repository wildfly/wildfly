/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

public class LogStoreParticipantDeleteHandler extends LogStoreParticipantOperationHandler {
    private LogStoreProbeHandler probeHandler = null;

    public LogStoreParticipantDeleteHandler(LogStoreProbeHandler probeHandler) {
        super("remove");

        this.probeHandler = probeHandler;
    }

    void refreshParticipant(OperationContext context) {
        final ModelNode operation = Util.createEmptyOperation("refresh-log-store", context.getCurrentAddress().getParent().getParent());

        context.addStep(operation, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                probeHandler.execute(context, operation);
            }
        }, OperationContext.Stage.MODEL);
    }
}
