/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jdr;

import java.util.function.Consumer;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Adds the JDR subsystem.
 *
 * @author Brian Stansberry
 */
public class JdrReportSubsystemAdd extends AbstractAddStepHandler {

    private final Consumer<JdrReportCollector> collectorConsumer;

    /**
     * Creates a new add handler for the JDR subsystem root resource.
     *
     * @param collectorConsumer consumer to pass a ref to the started JdrReportService to the rest of the subsystem. Cannot be {@code null}
     */
    JdrReportSubsystemAdd(final Consumer<JdrReportCollector> collectorConsumer) {
        this.collectorConsumer = collectorConsumer;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        JdrReportService.addService(context.getCapabilityServiceTarget(), collectorConsumer);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.getProcessType().isServer();
    }
}