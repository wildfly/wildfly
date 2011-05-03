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
package org.jboss.as.webservices.dmr.management;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.EndpointMetrics;
import org.jboss.wsf.spi.management.EndpointRegistry;

/**
 * To use this WS operation handler specify e.g. the following command in the CLI:
 *
 * <b>/subsystem=webservices:read-attribute(context=jaxws-samples-webmethod,endpoint=TestService,name="min-processing-time")</b>
 *
 * <p>
 * Supported <b>name</b> values are:
 * <ul>
 *   <li>start-time</li>
 *   <li>stop-time</li>
 *   <li>min-processing-time</li>
 *   <li>max-processing-time</li>
 *   <li>total-processing-time</li>
 *   <li>request-count</li>
 *   <li>response-count</li>
 *   <li>fault-count</li>
 * </ul>
 * </p>
 *
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSEndpointMetricsOperationHandler extends WSAbstractOperationHandler implements ModelQueryOperationHandler {

    public static final WSEndpointMetricsOperationHandler INSTANCE = new WSEndpointMetricsOperationHandler();
    public static final String[] ATTRIBUTES;

    private static final String CONTEXT = "context";
    private static final String ENDPOINT = "endpoint";
    private static final String NAME = "name";
    private static final String FALLBACK_MESSAGE = "No metrics available";

    private static final String START_TIME = "start-time";
    private static final String STOP_TIME = "stop-time";
    private static final String MIN_PROCESSING_TIME = "min-processing-time";
    private static final String MAX_PROCESSING_TIME = "max-processing-time";
    private static final String TOTAL_PROCESSING_TIME = "total-processing-time";
    private static final String REQUEST_COUNT = "request-count";
    private static final String RESPONSE_COUNT = "response-count";
    private static final String FAULT_COUNT = "fault-count";

    static {
        ATTRIBUTES = new String[] {
                START_TIME, STOP_TIME, MIN_PROCESSING_TIME, MAX_PROCESSING_TIME,
                TOTAL_PROCESSING_TIME, REQUEST_COUNT, RESPONSE_COUNT, FAULT_COUNT
        };
    }

    private WSEndpointMetricsOperationHandler() {
        // forbidden instantiation
    }

    protected ModelNode getManagementOperationResultFragment(final ModelNode operation, final ServiceController<?> controller) throws OperationFailedException {
        final String webContext = operation.require(CONTEXT).asString();
        final String endpointName = operation.require(ENDPOINT).asString();
        ObjectName endpointObjectName = null;
        try {
            endpointObjectName = new ObjectName("jboss.ws:context=" + webContext + ",endpoint=" + endpointName);
        } catch (final MalformedObjectNameException e) {
            throw new OperationFailedException(new ModelNode().set(e.getMessage()));
        }

        final String metricName = operation.require(NAME).asString();
        final EndpointRegistry registry = (EndpointRegistry) controller.getValue();
        final Endpoint endpoint = registry.getEndpoint(endpointObjectName);

        final ModelNode result = new ModelNode();
        if (endpoint != null && endpoint.getEndpointMetrics() != null) {
            final EndpointMetrics endpointMetrics = endpoint.getEndpointMetrics();
            if (START_TIME.equals(metricName)) {
                result.set(String.valueOf(endpointMetrics.getStartTime()));
            } else if (STOP_TIME.equals(metricName)) {
                result.set(String.valueOf(endpointMetrics.getStopTime()));
            } else if (MIN_PROCESSING_TIME.equals(metricName)) {
                result.set(String.valueOf(endpointMetrics.getMinProcessingTime()));
            } else if (MAX_PROCESSING_TIME.equals(metricName)) {
                result.set(String.valueOf(endpointMetrics.getMaxProcessingTime()));
            } else if (TOTAL_PROCESSING_TIME.equals(metricName)) {
                result.set(String.valueOf(endpointMetrics.getTotalProcessingTime()));
            } else if (REQUEST_COUNT.equals(metricName)) {
                result.set(String.valueOf(endpointMetrics.getRequestCount()));
            } else if (RESPONSE_COUNT.equals(metricName)) {
                result.set(String.valueOf(endpointMetrics.getResponseCount()));
            } else if (FAULT_COUNT.equals(metricName)) {
                result.set(String.valueOf(endpointMetrics.getFaultCount()));
            }
        } else {
            result.set(getFallbackMessage());
        }

        return result;
    }

    protected String getFallbackMessage() {
        return FALLBACK_MESSAGE;
    }

}
