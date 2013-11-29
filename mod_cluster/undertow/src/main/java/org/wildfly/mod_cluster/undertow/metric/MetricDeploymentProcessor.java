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
import io.undertow.servlet.api.ThreadSetupAction;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.extension.undertow.deployment.UndertowAttachments;

/**
 * {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} that registers metrics on deployment if mod_cluster
 * module is loaded.
 * <p/>
 * <ul>
 * <li>{@link RequestCountHttpHandler}</li>
 * <li>{@link RunningRequestsThreadSetupAction}</li>
 * <li>{@link BytesReceivedHttpHandler}</li>
 * <li>{@link BytesSentHttpHandler}</li>
 * </ul>
 *
 * @author Radoslav Husar
 * @since 8.0
 */
class MetricDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        List<HandlerWrapper> handlerAttachment = deploymentUnit.getAttachment(UndertowAttachments.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS);

        if (handlerAttachment == null) {
            handlerAttachment = new LinkedList<HandlerWrapper>();
            deploymentUnit.putAttachment(UndertowAttachments.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS, handlerAttachment);
        }

        // Request count wrapping
        handlerAttachment.add(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                return new RequestCountHttpHandler(handler);
            }
        });

        // Bytes Sent wrapping
        handlerAttachment.add(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                return new BytesSentHttpHandler(handler);
            }
        });

        // Bytes Received wrapping
        handlerAttachment.add(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                return new BytesReceivedHttpHandler(handler);
            }
        });

        // Busyness thread setup actions
        List<ThreadSetupAction> setupAttachment = deploymentUnit.getAttachment(UndertowAttachments.UNDERTOW_THREAD_SETUP_ACTIONS);

        if (setupAttachment == null) {
            setupAttachment = new LinkedList<ThreadSetupAction>();
            deploymentUnit.putAttachment(UndertowAttachments.UNDERTOW_THREAD_SETUP_ACTIONS, setupAttachment);
        }
        setupAttachment.add(new RunningRequestsThreadSetupAction());
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Do nothing.
    }

}
