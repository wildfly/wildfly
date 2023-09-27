/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jdr;

import static org.jboss.as.jdr.JdrReportSubsystemDefinition.JDR_CAPABILITY;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;

/**
 * Remove the JDR subsystem.
 *
 * @author Brian Stansberry
 */
public class JdrReportSubsystemRemove extends AbstractRemoveStepHandler {

    static final JdrReportSubsystemRemove INSTANCE = new JdrReportSubsystemRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        context.removeService(JDR_CAPABILITY.getCapabilityServiceName());
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.getProcessType().isServer();
    }
}