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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.deployment.client.api.DeploymentAction;
import org.jboss.as.deployment.client.api.domain.DeploymentPlan;

/**
 * Builder capable of creating a {@link DeploymentPlanImpl}.
 *
 * @author Brian Stansberry
 */
class AbstractDeploymentPlanBuilder  {

    final boolean globalRollback;

    private final List<DeploymentSetPlanImpl> setPlans = new ArrayList<DeploymentSetPlanImpl>();


    AbstractDeploymentPlanBuilder() {
        this.setPlans.add(new DeploymentSetPlanImpl());
        this.globalRollback = false;
    }
    AbstractDeploymentPlanBuilder(AbstractDeploymentPlanBuilder existing, boolean globalRollback) {
        this.setPlans.addAll(existing.setPlans);
        this.globalRollback = globalRollback;
    }

    AbstractDeploymentPlanBuilder(AbstractDeploymentPlanBuilder existing) {
        this.setPlans.addAll(existing.setPlans);
        this.globalRollback = existing.globalRollback;
    }

    AbstractDeploymentPlanBuilder(AbstractDeploymentPlanBuilder existing, DeploymentSetPlanImpl setPlan, boolean replace) {
        this(existing);
        if (replace) {
            this.setPlans.set(this.setPlans.size() - 1, setPlan);
        }
        else {
            this.setPlans.add(setPlan);
        }
    }

    public DeploymentAction getLastAction() {
        return getCurrentDeploymentSetPlan().getLastAction();
    }

    DeploymentSetPlanImpl getCurrentDeploymentSetPlan() {
        return setPlans.get(setPlans.size() - 1);
    }

    /**
     * Creates the deployment plan.
     *
     * @return the deployment plan
     */
    public DeploymentPlan build() {
        return new DeploymentPlanImpl(setPlans, globalRollback);
    }
}
