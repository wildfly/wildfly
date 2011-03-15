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

import java.util.Map;
import java.util.UUID;

/**
 * Encapsulates the results of executing a {@link DeploymentPlan}.
 *
 * @author Brian Stansberry
 */
public interface DeploymentPlanResult {

    /**
     * Gets the unique id of the deployment plan.
     *
     * @return the id. Will not be <code>null</code>
     */
    UUID getId();

    /**
     * Gets the deployment plan that lead to this result.
     *
     * @return the deployment plan. Will not be {@code null}
     */
    DeploymentPlan getDeploymentPlan();

    /**
     * Gets whether the deployment plan was valid. If {@code false} see
     * {@link #getInvalidDeploymentPlanException()} to for more information on
     * how the plan was invalid.
     *
     * @return <code>true</code> if the plan was valid; <code>false</code> otherwise
     */
    boolean isValid();

    /**
     * Gets the exception describing the problem with a deployment plan that
     * is not {@link #isValid() valid}.
     *
     * @return the exception or {@code null} if the plan is valid
     */
    InvalidDeploymentPlanException getInvalidDeploymentPlanException();

    /**
     * Gets the results for each server group.
     *
     * @return map of server group results, keyed by server group name
     */
    Map<String, ServerGroupDeploymentPlanResult> getServerGroupResults();

    /**
     * Gets the results of the {@link DeploymentAction}s associated with
     * the deployment set plan.
     *
     * @return map of deployment action results, keyed by {@link DeploymentAction#getId() deployment action id}
     */
    Map<UUID, DeploymentActionResult> getDeploymentActionResults();
}
