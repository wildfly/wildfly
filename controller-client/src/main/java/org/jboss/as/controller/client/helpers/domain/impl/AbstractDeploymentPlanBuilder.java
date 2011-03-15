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

import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlan;


/**
 * Builder capable of creating a {@link DeploymentPlanImpl}.
 *
 * @author Brian Stansberry
 */
class AbstractDeploymentPlanBuilder  {

    private final DeploymentSetPlanImpl setPlan;
    private final boolean rollbackAcrossGroups;

    AbstractDeploymentPlanBuilder() {
        this.setPlan = new DeploymentSetPlanImpl();
        this.rollbackAcrossGroups = false;
    }

    AbstractDeploymentPlanBuilder(AbstractDeploymentPlanBuilder existing, final boolean rollbackAcrossGroups) {
        this.setPlan = existing.setPlan;
        this.rollbackAcrossGroups = rollbackAcrossGroups;
    }

    AbstractDeploymentPlanBuilder(AbstractDeploymentPlanBuilder existing, DeploymentSetPlanImpl setPlan) {
        this.setPlan = setPlan;
        this.rollbackAcrossGroups = existing.rollbackAcrossGroups;
    }

    public DeploymentAction getLastAction() {
        return getCurrentDeploymentSetPlan().getLastAction();
    }

    DeploymentSetPlanImpl getCurrentDeploymentSetPlan() {
        return setPlan;
    }

    /**
     * Creates the deployment plan.
     *
     * @return the deployment plan
     */
    public DeploymentPlan build() {
        return new DeploymentPlanImpl(setPlan, rollbackAcrossGroups);
    }
}
