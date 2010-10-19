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

package org.jboss.as.deployment.client.impl.domain;

import org.jboss.as.deployment.client.api.domain.AddDeploymentPlanBuilder;
import org.jboss.as.deployment.client.api.domain.DeployDeploymentPlanBuilder;
import org.jboss.as.deployment.client.api.domain.ReplaceDeploymentPlanBuilder;
import org.jboss.as.deployment.client.impl.DeploymentActionImpl;

/**
 * Variant of a {@link DeploymentPlanBuilderImpl} that exposes
 * directives that are only applicable following an <code>add</code> directive.
 *
 * @author Brian Stansberry
 */
class AddDeploymentPlanBuilderImpl extends DeploymentPlanBuilderImpl implements AddDeploymentPlanBuilder  {

    private final String newContentKey;

    AddDeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentSetPlanImpl setPlan, boolean replace) {
        super(existing, setPlan, replace);
        this.newContentKey = setPlan.getLastAction().getDeploymentUnitUniqueName();
    }

    @Override
    public DeployDeploymentPlanBuilder andDeploy() {
        DeploymentActionImpl mod = DeploymentActionImpl.getDeployAction(newContentKey);
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        boolean add = currentSet.hasServerGroupPlans();
        DeploymentSetPlanImpl newSet = add ? new DeploymentSetPlanImpl() : currentSet;
        newSet = newSet.addAction(mod);
        return new DeployDeploymentPlanBuilderImpl(this, newSet, !add);
    }

    @Override
    public ReplaceDeploymentPlanBuilder andReplace(String toReplace) {
        return replace(newContentKey, toReplace);
    }
}
