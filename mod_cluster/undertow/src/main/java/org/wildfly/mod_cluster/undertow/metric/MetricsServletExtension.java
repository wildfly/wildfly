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

import javax.servlet.ServletContext;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * Extension that registers metrics on deployment if mod_cluster module is loaded.
 *
 * <ul>
 *     <li>{@link RequestCountHttpHandler}</li>
 *     <li>{@link RunningRequestsThreadSetupAction}</li>
 *     <li>{@link BytesSentThreadSetupAction}</li>
 *     <li>{@link BytesReceivedThreadSetupAction}</li>
 * </ul>
 *
 * @author Radoslav Husar
 * @version Aug 2013
 * @since 8.0
 */
public class MetricsServletExtension implements ServletExtension {

    @Override
    public void handleDeployment(final DeploymentInfo deploymentInfo, final ServletContext servletContext) {

        deploymentInfo
                // Request Count wrapping
                .addInitialHandlerChainWrapper(new HandlerWrapper() {
                    @Override
                    public HttpHandler wrap(final HttpHandler handler) {
                        return new RequestCountHttpHandler(handler);
                    }
                })

                // Busyness wrapping
                .addThreadSetupAction(new RunningRequestsThreadSetupAction())

                // Bytes Sent wrapping
                .addThreadSetupAction(new BytesSentThreadSetupAction())

                // Bytes Received wrapping
                .addThreadSetupAction(new BytesReceivedThreadSetupAction());
    }
}