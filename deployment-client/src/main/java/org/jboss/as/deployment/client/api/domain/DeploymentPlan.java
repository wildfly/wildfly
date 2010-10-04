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
 * Encapsulates an overall set of actions a {@link DomainDeploymentManager} should
 * take to update the set of deployment content available for deployment in the
 * domain and/or change the content deployed in the domain's servers.
 * <p>
 * A deployment plan may include zero or move {@link DeploymentAction.TYPE#ADD content addition actions},
 * zero or more {@link DeploymentAction.TYPE#REMOVE content removal actions}
 * and or zero or more {@link DeploymentSetPlan}s. Each <code>DeploymentSetPlan</code>
 * describes a set of changes to what should be deployed on the servers
 * in one or more server groups.
 * </p>
 */
public interface DeploymentPlan {

    /**
     * Gets the unique id of the plan.
     *
     * @return the id. Will not be <code>null</code>
     */
    UUID getId();

    /**
     * Gets the list of deployment repository content additions that are
     * part of the deployment plan.
     *
     * @return the actions. Will not be <code>null</code>
     */
    List<DeploymentAction> getAddedContent();

    /**
     * Gets the list of deployment repository content removals that are
     * part of the deployment plan.
     *
     * @return the actions. Will not be <code>null</code>
     */
    List<DeploymentAction> getRemovedContent();

    /**
     * Get the deployment set plans that comprise the plan.
     *
     * @return  the deployment set plans. Will not be <code>null</code>
     */
    List<DeploymentSetPlan> getDeploymentSetPlans();

    /**
     * Gets whether all {@link DeploymentSetPlan deployment sets} associated with the deployment plan
     * should be rolled back in case of a failure in any of them.
     *
     * @return <code>true</code> if all operations should be rolled back if
     *         any of them fail
     */
    boolean isGlobalRollback();

}
