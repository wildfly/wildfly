/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;

public class LogStoreParticipantRecoveryHandler extends LogStoreParticipantOperationHandler {
    private LogStoreParticipantRefreshHandler refreshHandler = null;

    public LogStoreParticipantRecoveryHandler(LogStoreParticipantRefreshHandler refreshHandler) {
        super("clearHeuristic");

        this.refreshHandler = refreshHandler;
    }

    // refresh the attributes of this participant (the status attribute should have changed to PREPARED
    void refreshParticipant(OperationContext context) {
        context.addStep(refreshHandler, OperationContext.Stage.MODEL, true);
    }
}
