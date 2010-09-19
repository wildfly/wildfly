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
import java.util.UUID;

import org.jboss.as.deployment.client.api.DeploymentAction;
import org.jboss.as.deployment.client.api.domain.DeploymentSetPlan;

/**
 * Describes a set of actions to take to change the deployment content available
 * to deployed in a server group or set of server groups.
 *
 * @author Brian Stansberry
 */
public class DeploymentSetPlanImpl implements DeploymentSetPlan {

    private final UUID uuid = UUID.randomUUID();
    private final List<DeploymentAction> deploymentActions = new ArrayList<DeploymentAction>();
    private final boolean rollback;
    private final boolean shutdown;
    private final long gracefulShutdownPeriod;

    DeploymentSetPlanImpl(List<DeploymentAction> actions, boolean rollback, boolean shutdown, long gracefulTimeout) {
        if (actions == null)
            throw new IllegalArgumentException("actions is null");
        this.deploymentActions.addAll(actions);
        this.rollback = rollback;
        this.shutdown = shutdown;
        this.gracefulShutdownPeriod = gracefulTimeout;
    }

    @Override
    public UUID getId() {
        return uuid;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.domain.DeploymentPlan#getDeploymentActions()
     */
    public List<DeploymentAction> getDeploymentActions() {
        return new ArrayList<DeploymentAction>(deploymentActions);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.domain.DeploymentPlan#isGlobalRollback()
     */
    public boolean isRollback() {
        return rollback;
    }

    @Override
    public long getGracefulShutdownTimeout() {
        return gracefulShutdownPeriod;
    }

    @Override
    public boolean isGracefulShutdown() {
        return shutdown && gracefulShutdownPeriod > -1;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }
}
