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

import org.jboss.as.controller.client.helpers.domain.RollbackDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlanBuilder;


/**
 * Variant of a {@link DeploymentPlanBuilderImpl} that exposes
 * directives that are only applicable when controlling how a {@link org.jboss.as.controller.client.helpers.domain.DeploymentSetPlan}
 * should be applied to one or more server groups.
 *
 * @author Brian Stansberry
 */
class ServerGroupDeploymentPlanBuilderImpl extends InitialDeploymentSetBuilderImpl implements ServerGroupDeploymentPlanBuilder {

    ServerGroupDeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentSetPlanImpl setPlan) {
        super(existing, setPlan);
    }

    @Override
    public RollbackDeploymentPlanBuilder withRollback() {
        DeploymentSetPlanImpl setPlan = getCurrentDeploymentSetPlan();
        ServerGroupDeploymentPlan groupPlan = setPlan.getLatestServerGroupDeploymentPlan();
        if (groupPlan == null) {
            throw MESSAGES.notConfigured(ServerGroupDeploymentPlan.class.getSimpleName());
        }
        groupPlan = groupPlan.createRollback();
        setPlan = setPlan.storeServerGroup(groupPlan);
        return new RollbackDeploymentPlanBuilderImpl(this, setPlan);
    }

    @Override
    public ServerGroupDeploymentPlanBuilder rollingToServers() {
        DeploymentSetPlanImpl setPlan = getCurrentDeploymentSetPlan();
        ServerGroupDeploymentPlan groupPlan = setPlan.getLatestServerGroupDeploymentPlan();
        if (groupPlan == null) {
            throw MESSAGES.notConfigured(ServerGroupDeploymentPlan.class.getSimpleName());
        }
        groupPlan = groupPlan.createRollingToServers();
        setPlan = setPlan.storeServerGroup(groupPlan);
        return new ServerGroupDeploymentPlanBuilderImpl(this, setPlan);
    }

    @Override
    public ServerGroupDeploymentPlanBuilder rollingToServerGroup(String serverGroupName) {
        DeploymentSetPlanImpl setPlan = getCurrentDeploymentSetPlan();
        ServerGroupDeploymentPlan groupPlan = new ServerGroupDeploymentPlan(serverGroupName);
        setPlan = setPlan.storeRollToServerGroup(groupPlan);
        return new ServerGroupDeploymentPlanBuilderImpl(this, setPlan);
    }

    @Override
    public ServerGroupDeploymentPlanBuilder toServerGroup(String serverGroupName) {
        DeploymentSetPlanImpl setPlan = getCurrentDeploymentSetPlan();
        ServerGroupDeploymentPlan groupPlan = new ServerGroupDeploymentPlan(serverGroupName);
        setPlan = setPlan.storeServerGroup(groupPlan);
        return new ServerGroupDeploymentPlanBuilderImpl(this, setPlan);
    }
}
