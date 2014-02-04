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

import org.jboss.as.controller.client.logging.ControllerClientLogger;
import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.RemoveDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.UndeployDeploymentPlanBuilder;



/**
 * Variant of a {@link DeploymentPlanBuilderImpl} that exposes
 * directives that are only applicable following an <code>undeploy</code> directive.
 *
 * @author Brian Stansberry
 */
class UndeployDeploymentPlanBuilderImpl extends DeploymentPlanBuilderImpl implements UndeployDeploymentPlanBuilder {

    private final DeploymentAction undeployModification;

    UndeployDeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentSetPlanImpl setPlan) {
        super(existing, setPlan);
        DeploymentAction modification = setPlan.getLastAction();
        if (modification.getType() != DeploymentAction.Type.UNDEPLOY) {
            throw ControllerClientLogger.ROOT_LOGGER.invalidActionType(modification.getType());
        }
        this.undeployModification = modification;
    }

    @Override
    public ServerGroupDeploymentPlanBuilder toServerGroup(String serverGroupName) {
        return super.toServerGroup(serverGroupName);
    }

    @Override
    public RemoveDeploymentPlanBuilder andRemoveUndeployed() {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw ControllerClientLogger.ROOT_LOGGER.cannotAddDeploymentActionsAfterStart();
        }
        DeploymentActionImpl mod = DeploymentActionImpl.getRemoveAction(undeployModification.getDeploymentUnitUniqueName());
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new RemoveDeploymentPlanBuilderImpl(this, newSet);
    }
}
