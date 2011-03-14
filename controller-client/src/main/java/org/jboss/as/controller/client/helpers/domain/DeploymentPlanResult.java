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
     *
     * @return
     */
    InvalidDeploymentPlanException getInvalidDeploymentPlanException();

    /**
     * Gets the result of a {@link DeploymentSetPlan}.
     *
     * @param deploymentSet the id of the deployment set plan
     *
     * @return the result. Will not be {@code null}
     *
     * @throws InvalidDeploymentPlanException if {@link #isValid()} would return <code>false</code>
     * @throws IllegalArgumentException if {@code deploymentSet} is not the
     * {@link DeploymentSetPlan#getId() id of a deployment set plan} associated
     * with the overall deployment plan.
     */
    DeploymentSetPlanResult getDeploymentSetResult(UUID deploymentSet) throws InvalidDeploymentPlanException;
}
