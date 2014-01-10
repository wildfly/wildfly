/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.mod_cluster.undertow.metric;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;
import org.jboss.modcluster.load.metric.impl.ReceiveTrafficLoadMetric;
import org.jboss.modcluster.load.metric.impl.RequestCountLoadMetric;
import org.jboss.modcluster.load.metric.impl.SendTrafficLoadMetric;
import org.wildfly.extension.undertow.deployment.UndertowAttachments;

/**
 * {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} that registers metrics on deployment if mod_cluster
 * module is loaded.
 * <p/>
 * <ul>
 * <li>{@link RequestCountHttpHandler}</li>
 * <li>{@link RunningRequestsHttpHandler}</li>
 * <li>{@link BytesReceivedHttpHandler}</li>
 * <li>{@link BytesSentHttpHandler}</li>
 * </ul>
 *
 * @author Radoslav Husar
 * @since 8.0
 */
class MetricDeploymentProcessor implements DeploymentUnitProcessor {

    private Set<LoadMetric> enabledMetrics;

    MetricDeploymentProcessor(Set<LoadMetric> enabledMetrics) {
        this.enabledMetrics = enabledMetrics;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        List<HandlerWrapper> handlerAttachment = deploymentUnit.getAttachment(UndertowAttachments.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS);

        if (handlerAttachment == null) {
            handlerAttachment = new LinkedList<HandlerWrapper>();
            deploymentUnit.putAttachment(UndertowAttachments.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS, handlerAttachment);
        }

        // Request count wrapping
        if (isMetricEnabled(RequestCountLoadMetric.class)) {
            handlerAttachment.add(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(final HttpHandler handler) {
                    return new RequestCountHttpHandler(handler);
                }
            });
        }

        // Bytes Sent wrapping
        if (isMetricEnabled(SendTrafficLoadMetric.class)) {
            handlerAttachment.add(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(final HttpHandler handler) {
                    return new BytesSentHttpHandler(handler);
                }
            });
        }

        // Bytes Received wrapping
        if (isMetricEnabled(ReceiveTrafficLoadMetric.class)) {
            handlerAttachment.add(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(final HttpHandler handler) {
                    return new BytesReceivedHttpHandler(handler);
                }
            });
        }

        // Busyness thread setup actions
        if (isMetricEnabled(BusyConnectorsLoadMetric.class)) {

            List<HandlerWrapper> outerHandlerAttachment = deploymentUnit.getAttachment(UndertowAttachments.UNDERTOW_OUTER_HANDLER_CHAIN_WRAPPERS);

            if (outerHandlerAttachment == null) {
                outerHandlerAttachment = new LinkedList<HandlerWrapper>();
                deploymentUnit.putAttachment(UndertowAttachments.UNDERTOW_OUTER_HANDLER_CHAIN_WRAPPERS, outerHandlerAttachment);
            }

            outerHandlerAttachment.add(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(final HttpHandler handler) {
                    return new RunningRequestsHttpHandler(handler);
                }
            });
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Do nothing.
    }

    /**
     * Checks whether this {@link Class} is configured to be used.
     *
     * @param metricClass
     * @return true if any of the enabled metrics is enabled, false otherwise
     */
    private boolean isMetricEnabled(Class metricClass) {
        for (LoadMetric enabledMetric : enabledMetrics) {
            if (metricClass.isInstance(enabledMetric)) {
                return true;
            }
        }

        return false;
    }

}
