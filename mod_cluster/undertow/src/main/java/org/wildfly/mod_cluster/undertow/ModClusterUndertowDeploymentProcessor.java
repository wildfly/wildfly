/*
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

package org.wildfly.mod_cluster.undertow;

import java.util.Set;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;
import org.jboss.modcluster.load.metric.impl.ReceiveTrafficLoadMetric;
import org.jboss.modcluster.load.metric.impl.RequestCountLoadMetric;
import org.jboss.modcluster.load.metric.impl.SendTrafficLoadMetric;
import org.wildfly.extension.undertow.deployment.UndertowAttachments;
import org.wildfly.mod_cluster.undertow.metric.BytesReceivedHttpHandler;
import org.wildfly.mod_cluster.undertow.metric.BytesSentHttpHandler;
import org.wildfly.mod_cluster.undertow.metric.RequestCountHttpHandler;
import org.wildfly.mod_cluster.undertow.metric.RunningRequestsHttpHandler;

/**
 * {@link DeploymentUnitProcessor} which adds a dependency on {@link UndertowEventHandlerAdapterServiceConfigurator}s to web
 * dependencies to support session draining (see <a href="https://issues.jboss.org/browse/WFLY-3942">WFLY-3942</a>) and
 * registers metrics on deployment if mod_cluster module is loaded.
 * <ul>
 * <li>{@link org.wildfly.mod_cluster.undertow.metric.RequestCountHttpHandler}</li>
 * <li>{@link org.wildfly.mod_cluster.undertow.metric.RunningRequestsHttpHandler}</li>
 * <li>{@link org.wildfly.mod_cluster.undertow.metric.BytesReceivedHttpHandler}</li>
 * <li>{@link org.wildfly.mod_cluster.undertow.metric.BytesSentHttpHandler}</li>
 * </ul>
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class ModClusterUndertowDeploymentProcessor implements DeploymentUnitProcessor {

    private final Set<String> adapterNames;
    private final Set<LoadMetric> enabledMetrics;

    public ModClusterUndertowDeploymentProcessor(Set<String> adapterNames, Set<LoadMetric> enabledMetrics) {
        this.adapterNames = adapterNames;
        this.enabledMetrics = enabledMetrics;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // Add mod_cluster-undertow integration service as a web deployment dependency
        for (String adapter : adapterNames) {
            deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, new UndertowEventHandlerAdapterServiceNameProvider(adapter).getServiceName());
        }

        // Request count wrapping
        if (isMetricEnabled(RequestCountLoadMetric.class)) {
            deploymentUnit.addToAttachmentList(UndertowAttachments.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS, RequestCountHttpHandler::new);
        }

        // Bytes Sent wrapping
        if (isMetricEnabled(SendTrafficLoadMetric.class)) {
            deploymentUnit.addToAttachmentList(UndertowAttachments.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS, BytesSentHttpHandler::new);
        }

        // Bytes Received wrapping
        if (isMetricEnabled(ReceiveTrafficLoadMetric.class)) {
            deploymentUnit.addToAttachmentList(UndertowAttachments.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS, BytesReceivedHttpHandler::new);
        }

        // Busyness thread setup actions
        if (isMetricEnabled(BusyConnectorsLoadMetric.class)) {
            deploymentUnit.addToAttachmentList(UndertowAttachments.UNDERTOW_OUTER_HANDLER_CHAIN_WRAPPERS, RunningRequestsHttpHandler::new);
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Do nothing.
    }

    /**
     * Checks whether this {@link Class} is configured to be used.
     *
     * @param metricClass Class to check whether it's configured to be used
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
