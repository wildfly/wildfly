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
import org.jboss.as.controller.client.helpers.domain.RollbackDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlanBuilder;


/**
 * Implementation of {@link RollbackDeploymentPlanBuilder}.
 *
 * @author Brian Stansberry
 */
class RollbackDeploymentPlanBuilderImpl extends ServerGroupDeploymentPlanBuilderImpl implements RollbackDeploymentPlanBuilder {

    RollbackDeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentSetPlanImpl setPlan) {
        super(existing, setPlan);
    }

    @Override
    public ServerGroupDeploymentPlanBuilder allowFailures(int serverFailures) {
        DeploymentSetPlanImpl setPlan = getCurrentDeploymentSetPlan();
        ServerGroupDeploymentPlan groupPlan = setPlan.getLatestServerGroupDeploymentPlan();
        if (groupPlan == null) {
            throw ControllerClientLogger.ROOT_LOGGER.notConfigured(ServerGroupDeploymentPlan.class.getSimpleName());
        }
        groupPlan = groupPlan.createAllowFailures(serverFailures);
        setPlan = setPlan.storeServerGroup(groupPlan);
        return new ServerGroupDeploymentPlanBuilderImpl(this, setPlan);
    }

    @Override
    public ServerGroupDeploymentPlanBuilder allowPercentageFailures(int serverFailurePercentage) {
        DeploymentSetPlanImpl setPlan = getCurrentDeploymentSetPlan();
        ServerGroupDeploymentPlan groupPlan = setPlan.getLatestServerGroupDeploymentPlan();
        if (groupPlan == null) {
            throw ControllerClientLogger.ROOT_LOGGER.notConfigured(ServerGroupDeploymentPlan.class.getSimpleName());
        }
        groupPlan = groupPlan.createAllowFailurePercentage(serverFailurePercentage);
        setPlan = setPlan.storeServerGroup(groupPlan);
        return new ServerGroupDeploymentPlanBuilderImpl(this, setPlan);
    }

}
