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
 * Results of a deployment operation.  Can be used to determine if the deployment was successful or failure as well as
 * collect any errors that occur during deployment.
 *
 * @author John E. Bailey
 */
public interface DeploymentResult {
    enum Result {SUCCESS, FAILURE};

    /**
     * Get teh result of the deployment.
     *
     * @return {@code Result.SUCCESS} if the deployment was successful or {@code Result.FAILURE} if not
     */
    Result getResult();

    /**
     * Get the deployment exception for a failed deployment
     *
     * @return the deployment exception
     */
    DeploymentException getDeploymentException();

    /**
     * Get a map of all the services that failed to start alon with the specific start exception.
     *
     * @return the map of start exceptions
     */
    Map<ServiceName, StartException> getServiceFailures();

    /**
     * Get the elapsed time fo the deployment.
     *
     * @return the elapsed deployment time
     */
    long getElapsedTime();

    /**
     * Future used to allow a deployment result to be retrieved at a later time.
     */
    interface Future {
        /**
         * Get the deployment result.  This call will block until the deployment result becomes available.
         * // TODO:  look at adding a timeout.
         *
         * @return The deployment result
         */
        DeploymentResult getDeploymentResult();
    }
}
