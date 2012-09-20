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

package org.jboss.as.controller.client.helpers.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.controller.client.DeploymentMetadata;

/**
 * Encapsulates an overall set of actions a {@link DomainDeploymentManager} should
 * take to update the set of deployment content available for deployment in the
 * domain and/or change the content deployed in the domain's servers.
 */
public interface DeploymentPlan {

    /**
     * Gets the unique id of the plan.
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
     * Indicates that on a given server all <code>deploy</code>, <code>undeploy</code> or
     * <code>replace</code> operations associated with the deployment set
     * should be rolled back in case of a failure in any of them.
     * <p>
     * <strong>Note:</strong> This directive does not span across servers, i.e.
     * a rollback on one server will not trigger rollback on others. Use
     * {@link ServerGroupDeploymentPlanBuilder#withRollback()} to trigger
     * rollback across servers.
     *
     * @return a builder that can continue building the overall deployment plan
     */
    boolean isSingleServerRollback();


    boolean isRollbackAcrossGroups();

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

    /**
     * Get the metadata associated with this deployment plan.
     *
     * @return The meta data.
     */
    DeploymentMetadata getMetadata();

    /**
     * Gets the configuration of how the {@link #getDeploymentActions() deployment
     * actions} are to be applied to the server groups in the domain. Each
     * {@link ServerGroupDeploymentPlan} in the returned data structure specifies
     * how the actions are to be applied to the servers within a particular server group. The data
     * structure itself is a list of sets of ServerGroupDeploymentPlans. Each
     * set indicates a collection of server groups to which actions can be
     * applied concurrently. Each element in the overall list delineates actions
     * should be applied in series.
     * <p>So, for example, assume we the overall deployment
     * set plan is intended to apply deployments to 3 server groups: <code>A</code>,
     * <code>B</code> and <code>C</code>. Assume elements within curly braces
     * represent and set and elements within brackets represent an item in a list:
     * <ul>
     * <li> <code>[{A,B}],[{C}]</code> would describe a plan to concurrently
     * execute the deployment actions on server groups A and B and then when A
     * and B are complete, continue on to server group C.</li>
     * <li> <code>[{A}],[{B}],[{C}]</code> would describe a plan to
     * execute the deployment actions on server group A, and then when A
     * is complete, continue on to server group B and then to C.</li>
     * <li> <code>[{A,B,C}]</code> would describe a plan to concurrently
     * execute the deployment actions on server groups A and B and C.</li>
     * </ul>
     * </p>
     *
     * @return data structure representing how the {@link #getDeploymentActions() deployment
     * actions} are to be applied to the server groups in the domain.
     */
    List<Set<ServerGroupDeploymentPlan>> getServerGroupDeploymentPlans();

}
