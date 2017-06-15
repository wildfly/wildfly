/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jdr;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Operation handler for an end user request to generate a JDR report.
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 */
public class JdrReportRequestHandler implements OperationStepHandler {

    private static final String OPERATION_NAME = "generate-jdr-report";

    static final JdrReportRequestHandler INSTANCE = new JdrReportRequestHandler();

    static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, JdrReportExtension.getResourceDescriptionResolver())
            .setReplyParameters(CommonAttributes.START_TIME, CommonAttributes.END_TIME, CommonAttributes.REPORT_LOCATION)
            .setReadOnly()
            .setRuntimeOnly()
            .addAccessConstraint(JdrReportExtension.JDR_SENSITIVITY_DEF)
            .build();

    private final ParametersValidator validator = new ParametersValidator();

    private JdrReportRequestHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // In MODEL stage, just validate the request. Unnecessary if the request has no parameters
        validator.validate(operation);

        // Register a handler for the RUNTIME stage
        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                ServiceRegistry registry = context.getServiceRegistry(false);
                JdrReportCollector jdrCollector = JdrReportCollector.class.cast(registry.getRequiredService(JdrReportService.SERVICE_NAME).getValue());

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
