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

import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ReplaceDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlanBuilder;


/**
 * Variant of a {@link DeploymentPlanBuilderImpl} that exposes
 * directives that are only applicable following a <code>replace</code> directive.
 *
 * @author Brian Stansberry
 */
class ReplaceDeploymentPlanBuilderImpl extends DeploymentPlanBuilderImpl implements ReplaceDeploymentPlanBuilder {

    private final DeploymentAction replacementModification;

    ReplaceDeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentSetPlanImpl setPlan) {
        super(existing, setPlan);
        this.replacementModification = setPlan.getLastAction();
    }

    @Override
    public ServerGroupDeploymentPlanBuilder toServerGroup(String serverGroupName) {
        return super.toServerGroup(serverGroupName);
    }

    @Override
    public DeploymentPlanBuilder andRemoveUndeployed() {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        DeploymentActionImpl mod = DeploymentActionImpl.getRemoveAction(replacementModification.getReplacedDeploymentUnitUniqueName());
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new DeploymentPlanBuilderImpl(this, newSet);
    }
}
