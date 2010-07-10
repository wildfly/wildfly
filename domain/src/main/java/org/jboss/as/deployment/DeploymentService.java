/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.as.deployment.unit.DeploymentChain;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VirtualFile;

import java.lang.reflect.Method;

/**
 * Service that represents a deployment.  Should be used as a dependency for all services registered for the deployment.
 *
 * @author John E. Bailey
 */
public class DeploymentService implements Service<DeploymentService> {
    public static final ServiceName DEPLOYMENT_SERVICE_NAME = ServiceName.JBOSS.append("deployment");
    public static final Method DEPLOYMENT_ROOT_SETTER;
    static {
        try {
            DEPLOYMENT_ROOT_SETTER = DeploymentService.class.getMethod("setDeploymentRoot", VirtualFile.class);
        } catch(NoSuchMethodException e) {
            throw new RuntimeException(e);  // Gross....
        }
    }

    private VirtualFile deploymentRoot;
    private DeploymentChain deploymentChain;
    private String deploymentName;

    @Override
    public void start(StartContext context) throws StartException {
        // Root should be mounted at this point
        this.deploymentName = deploymentRoot.getPathName();

        // Determine the deployment chain
        this.deploymentChain = determineDeploymentChain(deploymentRoot);
    }

    private DeploymentChain determineDeploymentChain(VirtualFile deploymentRoot) {
        return null;
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public DeploymentService getValue() throws IllegalStateException {
        return this;
    }

    public void setDeploymentRoot(final VirtualFile deploymentRoot) {
        this.deploymentRoot = deploymentRoot;
    }

    public VirtualFile getDeploymentRoot() {
        return deploymentRoot;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public DeploymentChain getDeploymentChain() {
        return deploymentChain;
    }
}
