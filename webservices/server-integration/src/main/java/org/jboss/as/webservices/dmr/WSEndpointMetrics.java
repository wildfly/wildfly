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
package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import javax.management.ObjectName;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.EndpointMetrics;
import org.jboss.wsf.spi.management.EndpointRegistry;

/**
* WSEndpointsMetrics to get the endpoint start/stop time and processing time
*
* @author <a href="mailto:ema@redhat.com">Jim Ma</a>
*/
public class WSEndpointMetrics implements ModelQueryOperationHandler {

    static WSEndpointMetrics INSTANCE = new WSEndpointMetrics();

    static final String[] NO_LOCATION = new String[0];

    private static final String START_TIME = "startTime";
    private static final String STOP_TIME = "stopTime";
    private static final String MIN_PROCESSING_TIME = "minProcessingTime";
    private static final String MAX_PROCESSING_TIME = "maxProcessingTime";
    private static final String TOTAL_PROCESSING_TIME = "totalProcessingTime";
    private static final String REQUEST_COUNT = "requestCount";
    private static final String RESPONSE_COUNT = "responseCount";
    private static final String FAULT_COUNT = "faultCount";
    static final String[] ATTRIBUTES = new String[] { START_TIME, STOP_TIME, MIN_PROCESSING_TIME, MAX_PROCESSING_TIME,
            TOTAL_PROCESSING_TIME, REQUEST_COUNT, RESPONSE_COUNT, FAULT_COUNT };
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final String attributeName = operation.require(NAME).asString();
                    final String webContext = operation.require("context").asString();
                    final String endpoint = operation.require("endpoint").asString();

                    final ServiceController<?> controller = context.getServiceRegistry()
                            .getService(WSServices.REGISTRY_SERVICE);
                    if (controller != null) {
                        try {
                            final EndpointRegistry registry = (EndpointRegistry) controller.getValue();
                            final ModelNode result = new ModelNode();
                            ObjectName endpointName = new ObjectName("jboss.ws:context=" + webContext + ",endpoint=" + endpoint);

                            Endpoint ep = registry.getEndpoint(endpointName);
                            if (ep != null && ep.getEndpointMetrics() != null) {
                                EndpointMetrics epMt = ep.getEndpointMetrics();
                                if (START_TIME.equals(attributeName)) {
                                    result.set("" + epMt.getStartTime());
                                } else if (STOP_TIME.equals(attributeName)) {
                                    result.set("" + epMt.getStopTime());
                                } else if (MIN_PROCESSING_TIME.equals(attributeName)) {
                                    result.set("" + epMt.getMinProcessingTime());
                                } else if (MAX_PROCESSING_TIME.equals(attributeName)) {
                                    result.set("" + epMt.getMaxProcessingTime());
                                } else if (TOTAL_PROCESSING_TIME.equals(attributeName)) {
                                    result.set("" + epMt.getTotalProcessingTime());
                                } else if (REQUEST_COUNT.equals(attributeName)) {
                                    result.set("" + epMt.getRequestCount());
                                } else if (RESPONSE_COUNT.equals(attributeName)) {
                                    result.set("" + epMt.getResponseCount());
                                } else if (FAULT_COUNT.equals(attributeName)) {
                                    result.set("" + epMt.getFaultCount());
                                }
                            } else {
                                result.set("", "no endpoint avaiable");
                            }
                            resultHandler.handleResultFragment(new String[0], result);
                            resultHandler.handleResultComplete();
                        } catch (Exception e) {
                            throw new OperationFailedException(new ModelNode().set("failed to get metrics" + e.getMessage()));
                        }
                    } else {
                        resultHandler.handleResultFragment(NO_LOCATION, new ModelNode().set("no metrics available"));
                        resultHandler.handleResultComplete();
                    }
                }
            });
        } else {
            resultHandler.handleResultFragment(NO_LOCATION, new ModelNode().set("no metrics available"));
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult();
    }
}
