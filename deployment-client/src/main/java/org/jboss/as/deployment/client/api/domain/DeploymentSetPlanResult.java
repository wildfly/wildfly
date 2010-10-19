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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import org.jboss.as.deployment.client.api.DeploymentAction;

/**
 * Encapsulates the results of executing a {@link DeploymentSetPlan}.
 *
 * @author Brian Stansberry
 */
public interface DeploymentSetPlanResult {

    /**
     * Gets the unique id of the deployment set plan.
     *
     * @return the id. Will not be <code>null</code>
     */
    UUID getDeploymentSetId();

    /**
     * Gets the results for each server group.
     *
     * @return map of server group results, keyed by server group name
     */
    Map<String, ServerGroupDeploymentPlanResult> getServerGroupsResults();

    /**
     * Gets the result of a {@link DeploymentAction} associated with
     * the deployment set plan.
     *
     * @param deploymentAction the id of the action
     *
     * @return the result
     */
    Future<DeploymentActionResult> getDeploymentActionResult(UUID deploymentAction);
}
