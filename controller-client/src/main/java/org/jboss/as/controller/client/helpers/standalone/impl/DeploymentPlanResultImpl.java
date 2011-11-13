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

package org.jboss.as.controller.client.helpers.standalone.impl;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;

/**
 * Default implementation of  {@link ServerDeploymentPlanResult}.
 *
 * @author Brian Stansberry
 */
public class DeploymentPlanResultImpl implements ServerDeploymentPlanResult, Serializable {

    private static final long serialVersionUID = -2473360314683299361L;

    private final Map<UUID, ServerDeploymentActionResult> actionResults;
    private final UUID planId;

    public DeploymentPlanResultImpl(UUID planId, Map<UUID, ServerDeploymentActionResult> actionResults) {
        if (planId == null)
            throw MESSAGES.nullVar("planId");
        if (actionResults == null)
            throw MESSAGES.nullVar("actionResults");
        this.planId = planId;
        this.actionResults = actionResults;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.server.DeploymentPlanResult#getDeploymentActionResult(java.util.UUID)
     */
    @Override
    public ServerDeploymentActionResult getDeploymentActionResult(UUID deploymentAction) {
        return actionResults.get(deploymentAction);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.server.DeploymentPlanResult#getDeploymentPlanId()
     */
    @Override
    public UUID getDeploymentPlanId() {
        return planId;
    }

}
