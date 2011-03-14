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

package org.jboss.as.controller.client.helpers.domain.impl;

import java.util.Map;
import java.util.UUID;

import org.jboss.as.controller.client.helpers.domain.DeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.DeploymentSetPlanResult;
import org.jboss.as.controller.client.helpers.domain.InvalidDeploymentPlanException;


/**
 * Default implementation of {@link DeploymentPlanResult}.
 *
 * @author Brian Stansberry
 */
public class DeploymentPlanResultImpl implements DeploymentPlanResult {

    private final DeploymentPlan plan;
    private final InvalidDeploymentPlanException idpe;
    private final Map<UUID, DeploymentSetPlanResult> setResults;

    public DeploymentPlanResultImpl(final DeploymentPlan plan, final Map<UUID, DeploymentSetPlanResult> setResults) {
        if (plan == null)
            throw new IllegalArgumentException("plan is null");
        if (setResults == null)
            throw new IllegalArgumentException("setResults is null");
        this.plan = plan;
        this.setResults = setResults;
        this.idpe = null;
    }

    public DeploymentPlanResultImpl(final DeploymentPlan plan, final InvalidDeploymentPlanException invalidPlanException) {
        if (plan == null)
            throw new IllegalArgumentException("plan is null");
        if (invalidPlanException == null)
            throw new IllegalArgumentException("invalidPlanException is null");
        this.plan = plan;
        this.setResults = null;
        this.idpe = invalidPlanException;
    }

    @Override
    public DeploymentSetPlanResult getDeploymentSetResult(UUID deploymentSet) throws InvalidDeploymentPlanException {
        if (idpe != null)
            throw idpe;
        return setResults.get(deploymentSet);
    }

    @Override
    public UUID getId() {
        return plan.getId();
    }

    @Override
    public DeploymentPlan getDeploymentPlan() {
        return plan;
    }

    @Override
    public InvalidDeploymentPlanException getInvalidDeploymentPlanException() {
        return idpe;
    }

    @Override
    public boolean isValid() {
        return idpe == null;
    }

}
