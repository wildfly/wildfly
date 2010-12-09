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

package org.jboss.as.server.deployment;

import java.io.Closeable;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VFSUtils;

/**
 * Service that represents a deployment. Should be used as a dependency for all services registered for the deployment. The
 * life-cycle of this service should be used to control the life-cycle of the deployment.
 *
 * @author John E. Bailey
 */
public class DeploymentService implements Service<Void> {
    private static final Logger log = Logger.getLogger("org.jboss.as.deployment");
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deployment");

    private final Closeable deploymentMount;

    public DeploymentService(Closeable deploymentMount) {
        if (deploymentMount == null) {
            throw new IllegalArgumentException("deploymentMount is null");
        }
        this.deploymentMount = deploymentMount;
    }

    public static ServiceName getServiceName(String deploymentName) {
        return DeploymentService.SERVICE_NAME.append(deploymentName);
    }

    /**
     * Start the deployment. This will re-mount the deployment root if service is restarted.
     *
     * @param context The start context
     * @throws StartException if any problems occur
     */
    public void start(StartContext context) throws StartException {
        log.debugf("Start deployment service: %s", deploymentMount);
    }

    /**
     * Stop the deployment. This will close the virtual file mount.
     *
     * @param context The stop context
     */
    public void stop(StopContext context) {
        log.debugf("Stop deployment service: %s", deploymentMount);
        VFSUtils.safeClose(deploymentMount);
    }

    /** {@inheritDoc} **/
    public Void getValue() throws IllegalStateException {
        return null;
    }
}
