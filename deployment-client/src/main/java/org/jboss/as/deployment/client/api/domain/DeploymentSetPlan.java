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

package org.jboss.as.deployment.client.api.domain;

import java.util.List;
import java.util.UUID;

import org.jboss.as.deployment.client.api.DeploymentAction;

/**
 * Encapsulates a related set of actions a {@link DomainDeploymentManager} should
 * take to change the content deployed in the servers associated with one or
 * more server groups.
 *
 * @author Brian Stansberry
 */
public interface DeploymentSetPlan {

    /**
     * Gets the unique id of the deployment set plan.
     *
     * @return the id. Will not be <code>null</code>
     */
    UUID getId();

    /**
     * Gets the list of deploy, replace and undeploy actions that are part
     * of the deployment plan.
     *
     * @return  the actions. Will not be <code>null</code>
     */
    List<DeploymentAction> getDeploymentActions();

    /**
     * Gets whether all <code>deploy</code>, <code>undeploy</code>, <code>replace</code>
     * or <code>remove</code> operations associated with the deployment set plan
     * should be rolled back in case of a failure in any of them.
     *
     * @return <code>true</code> if all operations should be rolled back if
     *         any of them fail
     */
    boolean isRollback();

    /**
     * Gets whether the deployment set plan is organized around a shutdown of the server.
     *
     * @return <code>true</code> if the plan will be organized around a shutdown,
     *         <code>false</code> otherwise
     */
    boolean isShutdown();

    /**
     * Gets whether the deployment set plan is organized around
     * a graceful shutdown of the server, where potentially long-running in-process
     * work is given time to complete before shutdown proceeds.
     *
     * @return <code>true</code> if the plan will be organized around a graceful shutdown,
     *         <code>false</code> otherwise
     */
    boolean isGracefulShutdown();

    /**
     * Gets the maximum period, in ms, the deployment set plan is configured to
     * wait for potentially long-running in-process work ito complete before
     * shutdown proceeds.
     *
     * @return the period in ms, or <code>-1</code> if {@link #isGracefulShutdown()}
     *         would return <code>false</code>
     */
    long getGracefulShutdownTimeout();

}
