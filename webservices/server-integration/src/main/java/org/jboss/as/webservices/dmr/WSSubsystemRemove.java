/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.dmr;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class WSSubsystemRemove extends ReloadRequiredRemoveStepHandler {

    static final WSSubsystemRemove INSTANCE = new WSSubsystemRemove();

    private WSSubsystemRemove() {

    }
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performRuntime(context, operation, model);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        super.recoverServices(context, operation, model);

    }
}
