/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jdr;

import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Operation handler for an end user request to generate a JDR report.
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 */
public class JdrReportRequestHandler implements OperationStepHandler {

    private static final String OPERATION_NAME = "generate-jdr-report";

    static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, JdrReportExtension.SUBSYSTEM_RESOLVER)
            .setReplyParameters(CommonAttributes.START_TIME, CommonAttributes.END_TIME, CommonAttributes.REPORT_LOCATION)
            .setReadOnly()
            .setRuntimeOnly()
            .addAccessConstraint(JdrReportExtension.JDR_SENSITIVITY_DEF)
            .build();

    private final Supplier<JdrReportCollector> collectorSupplier;

    /**
     * Create a new handler for the {@code generate-jdr-report} operation.
     *
     * @param collectorSupplier supplier of the {@link JdrReportCollector} to use to collect the report. Cannot be {@code null}.
     */
    JdrReportRequestHandler(Supplier<JdrReportCollector> collectorSupplier) {
        this.collectorSupplier = collectorSupplier;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Register a handler for the RUNTIME stage
        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                JdrReportCollector jdrCollector = collectorSupplier.get();
                if (jdrCollector == null) {
                    // JdrReportService must have failed or been stopped
                    throw new IllegalStateException();
                }

                ModelNode response = context.getResult();
                JdrReport report = jdrCollector.collect();

                if (report.getStartTime() != null) {
                    response.get("start-time").set(report.getStartTime());
                }
                if (report.getEndTime() != null) {
                    response.get("end-time").set(report.getEndTime());
                }
                response.get("report-location").set(report.getLocation());

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
