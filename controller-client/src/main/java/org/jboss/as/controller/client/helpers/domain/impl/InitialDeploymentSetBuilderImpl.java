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

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.helpers.domain.DeploymentSetPlan;
import org.jboss.as.controller.client.helpers.domain.InitialDeploymentSetBuilder;


/**
 * Variant of a {@link DeploymentPlanBuilderImpl} that is meant
 * to be used at the initial stages of the building process, when directives that
 * pertain to the entire {@link DeploymentSetPlan} can be applied.
 *
 * @author Brian Stansberry
 */
public class InitialDeploymentSetBuilderImpl extends DeploymentPlanBuilderImpl implements InitialDeploymentSetBuilder  {

    /**
     * Constructs a new InitialDeploymentPlanBuilder
     */
    InitialDeploymentSetBuilderImpl(DeploymentContentDistributor deploymentDistributor) {
        super(deploymentDistributor);
    }

    InitialDeploymentSetBuilderImpl(DeploymentPlanBuilderImpl existing, boolean globalRollback) {
        super(existing, globalRollback);
    }

    InitialDeploymentSetBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentSetPlanImpl setPlan) {
        super(existing, setPlan);
    }

    @Override
    public InitialDeploymentSetBuilder withGracefulShutdown(long timeout, TimeUnit timeUnit) {

        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        long period = timeUnit.toMillis(timeout);
        if (currentSet.isShutdown() && period != currentSet.getGracefulShutdownTimeout()) {
            throw MESSAGES.gracefulShutdownAlreadyConfigured(currentSet.getGracefulShutdownTimeout());
        }
        DeploymentSetPlanImpl newSet = currentSet.setGracefulTimeout(period);
        return new InitialDeploymentSetBuilderImpl(this, newSet);
    }

    @Override
    public InitialDeploymentSetBuilder withShutdown() {

        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        DeploymentSetPlanImpl newSet = currentSet.setShutdown();
        return new InitialDeploymentSetBuilderImpl(this, newSet);
    }

    @Override
    @Deprecated
    public InitialDeploymentSetBuilder withSingleServerRollback() {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        DeploymentSetPlanImpl newSet = currentSet.setRollback();
        return new InitialDeploymentSetBuilderImpl(this, newSet);
    }

    @Override
    public InitialDeploymentSetBuilder withoutSingleServerRollback() {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        DeploymentSetPlanImpl newSet = currentSet.setNoRollback();
        return new InitialDeploymentSetBuilderImpl(this, newSet);
    }
}
