/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * Names related to a deployment.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class DeploymentNames {
    public static final AttachmentKey<DeploymentNames> ATTACHMENT_KEY = AttachmentKey.create(DeploymentNames.class);

    private final String servletContainerName;
    private final String deploymentName;
    private final String hostName;
    private final String contextPath;
    private final String serverName;

    public DeploymentNames(String serverName, String containerName, String hostName, String deploymentName, String pathName) {
        super();
        this.serverName = serverName;
        this.servletContainerName = containerName;
        this.hostName = hostName;
        this.deploymentName = deploymentName;
        this.contextPath = pathName;
    }

    /**
     * @return the name of the Undertow servlet container associated with this deployment
     */
    public String getServletContainerName() {
        return servletContainerName;
    }

    /**
     * @return the name under which this deployment is known in the management model
     */
    public String getDeploymentName() {
        return deploymentName;
    }

    /**
     * @return the name of the Undertow vitual host on which this deployment is deployed
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @return the context path under which this deployment is available
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * @return the name of the Undertow server on which this deployment is deployed
     */
    public String getServerName() {
        return serverName;
    }

}
