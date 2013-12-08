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

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.wildfly.extension.undertow.deployment.UndertowHandlerWrapperDeploymentProcessor;

/**
 * @author Radoslav Husar
 * @since 8.0
 */
public class ModClusterUndertowMetricDeploymentUnitProcessor implements UndertowHandlerWrapperDeploymentProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        List<HandlerWrapper> attachment = deploymentUnit.getAttachment(UndertowHandlerWrapperDeploymentProcessor.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS);

        if (attachment == null) {
            attachment = new LinkedList<HandlerWrapper>();
            deploymentUnit.putAttachment(UndertowHandlerWrapperDeploymentProcessor.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS, attachment);
        }

        attachment.add(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                return new RequestCountHttpHandler(handler);
            }
        });

        // Busyness wrapping
        //.addThreadSetupAction(new RunningRequestsThreadSetupAction())

        // Bytes Sent wrapping
        //.addThreadSetupAction(new BytesSentThreadSetupAction())

        // Bytes Received wrapping
        //.addThreadSetupAction(new BytesReceivedThreadSetupAction());
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Do nothing.
    }

}
