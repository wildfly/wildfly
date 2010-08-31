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

package org.jboss.as.deployment;

import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service listener used by deployment to handle deployment service errors.  Will stop the deployment service if any service
 * start exceptions occur.
 *
 * TODO:  Make this configurable.  Determine if partial starts are allowed.
 *
 * @author John E. Bailey
 */
public class DeploymentFailureListener extends AbstractServiceListener<Object>{
    private static final Logger log = Logger.getLogger("org.jboss.as.deployment");
    private final AtomicBoolean deploymentStopped = new AtomicBoolean();
    private final ServiceName deploymentServiceName;

    /**
     * Construct new instance with a deployment service name.
     * 
     * @param deploymentServiceName The deployment service name
     */
    public DeploymentFailureListener(final ServiceName deploymentServiceName) {
        this.deploymentServiceName = deploymentServiceName;
    }

    /** {@inheritDoc} */
    public void serviceFailed(ServiceController<? extends Object> serviceController, StartException reason) {
        if(deploymentStopped.compareAndSet(false, true)) {
            log.errorf("Deployment [%s] failed to start correctly.  Completely shutting down deployment.  Please see additional errors for details.", deploymentServiceName);
            final ServiceContainer serviceContainer = serviceController.getServiceContainer();
            final ServiceController<?> deploymentService = serviceContainer.getService(deploymentServiceName);
            if(deploymentService != null) {
                deploymentService.setMode(ServiceController.Mode.NEVER);
            }
        }
    }
}
