/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.RequestGroupInfo;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Emanuel Muckenhuber
 */
class WebConnectorMetrics implements NewStepHandler {

    static WebConnectorMetrics INSTANCE = new WebConnectorMetrics();

    static final String[] NO_LOCATION = new String[0];
    private static final String BYTES_SENT = "bytesSent";
    private static final String BYTES_RECEIVED = "bytesReceived";
    private static final String PROCESSING_TIME = "processingTime";
    private static final String ERROR_COUNT = "errorCount";
    private static final String MAX_TIME = "maxTime";
    private static final String REQUEST_COUNT = "requestCount";
    static final String[] ATTRIBUTES = new String[] {BYTES_SENT, BYTES_RECEIVED, PROCESSING_TIME, ERROR_COUNT, MAX_TIME, REQUEST_COUNT};

    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.getType() == NewOperationContext.Type.SERVER) {
            context.addStep(new NewStepHandler() {
                public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                    final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                    final String name = address.getLastElement().getValue();
                    final String attributeName = operation.require(NAME).asString();

                    final ServiceController<?> controller = context.getServiceRegistry()
                            .getService(WebSubsystemServices.JBOSS_WEB_CONNECTOR.append(name));
                    if (controller != null) {
                        try {
                            final Connector connector = (Connector) controller.getValue();
                            final ModelNode result = context.getResult();
                            if (connector.getProtocolHandler() != null && connector.getProtocolHandler().getRequestGroupInfo() != null) {
                                RequestGroupInfo info = connector.getProtocolHandler().getRequestGroupInfo();
                                if (BYTES_SENT.equals(attributeName)) {
                                    result.set("" + info.getBytesSent());
                                } else if (BYTES_RECEIVED.equals(attributeName)) {
                                    result.set("" + info.getBytesReceived());
                                } else if (PROCESSING_TIME.equals(attributeName)) {
                                    result.set("" + info.getProcessingTime());
                                } else if (ERROR_COUNT.equals(attributeName)) {
                                    result.set("" + info.getErrorCount());
                                } else if (MAX_TIME.equals(attributeName)) {
                                    result.set("" + info.getMaxTime());
                                } else if (REQUEST_COUNT.equals(attributeName)) {
                                    result.set("" + info.getRequestCount());
                                }
                            }
                        } catch (Exception e) {
                            throw new OperationFailedException(new ModelNode().set("failed to get metrics" + e.getMessage()));
                        }
                    } else {
                        context.getResult().set("no metrics available");
                    }
                    context.completeStep();
                }
            }, NewOperationContext.Stage.RUNTIME);
        } else {
            context.getResult().set("no metrics available");
        }
        context.completeStep();
    }
}
