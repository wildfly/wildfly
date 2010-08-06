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

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

import java.util.Map;

/**
 * Deployment result implementation.
 *  
 * @author John E. Bailey
 */
public class DeploymentResultImpl implements DeploymentResult {
    private final Result result;
    private final DeploymentException deploymentException;
    private final Map<ServiceName, StartException> serviceFailures;
    private final long elapsedTime;
    private final int numServices;

    public DeploymentResultImpl(Result result, DeploymentException deploymentException, Map<ServiceName, StartException> serviceFailures, long elapsedTime, int numServices) {
        this.result = result;
        this.deploymentException = deploymentException;
        this.serviceFailures = serviceFailures;
        this.elapsedTime = elapsedTime;
        this.numServices = numServices;
    }

    @Override
    public Result getResult() {
        return result;
    }

    @Override
    public DeploymentException getDeploymentException() {
        return deploymentException;
    }

    @Override
    public Map<ServiceName, StartException> getServiceFailures() {
        return serviceFailures;
    }

    @Override
    public long getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public int getNumberOfServicesDeployed() {
        return numServices;
    }

    /**
     * Future implementation.  Uses {@code Object.wait} semantics to control the future.
     */
    public static class FutureImpl implements Future {
        private DeploymentResult deploymentResult;

        @Override
        public DeploymentResult getDeploymentResult() {
            boolean intr = false;
            try {
                DeploymentResult deploymentResult = this.deploymentResult;
                if (deploymentResult == null) synchronized (this) {
                    while ((deploymentResult = this.deploymentResult) == null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            intr = true;
                        }
                    }
                }
                return deploymentResult;
            } finally {
                if (intr) Thread.currentThread().interrupt();
            }
        }

        public void setDeploymentResult(DeploymentResult deploymentResult) {
            synchronized (this) {
                this.deploymentResult = deploymentResult;
                notifyAll();
            }
        }
    }
}
