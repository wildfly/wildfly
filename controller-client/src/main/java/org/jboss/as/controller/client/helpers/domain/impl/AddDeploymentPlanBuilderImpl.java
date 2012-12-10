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

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import org.jboss.as.controller.client.helpers.domain.AddDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.DeployDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ReplaceDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlanBuilder;


/**
 * Variant of a {@link DeploymentPlanBuilderImpl} that exposes
 * directives that are only applicable following an <code>add</code> directive.
 *
 * @author Brian Stansberry
 */
class AddDeploymentPlanBuilderImpl extends DeploymentPlanBuilderImpl implements AddDeploymentPlanBuilder  {

    private final String newContentKey;

    AddDeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentSetPlanImpl setPlan) {
        super(existing, setPlan);
        this.newContentKey = setPlan.getLastAction().getDeploymentUnitUniqueName();
    }

    @Override
    public DeployDeploymentPlanBuilder andDeploy() {
        return this.andDeploy(null);
    }

    @Override
    public DeployDeploymentPlanBuilder andDeploy(String policy) {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        DeploymentActionImpl mod = DeploymentActionImpl.getDeployAction(newContentKey, policy);
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new DeployDeploymentPlanBuilderImpl(this, newSet);
    }

    @Override
    public ReplaceDeploymentPlanBuilder andReplace(String toReplace) {
        return replace(newContentKey, toReplace);
    }

    @Override
    public ServerGroupDeploymentPlanBuilder toServerGroup(String serverGroupName) {
        return super.toServerGroup(serverGroupName);
    }
}
