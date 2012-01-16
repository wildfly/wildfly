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

package org.jboss.as.controller.client.helpers.standalone.impl;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.protocol.StreamUtils;

/**
 * Describes a set of actions to take to change the deployment content available
 * to and/or deployed in a standalone server.
 *
 * @author Brian Stansberry
 */
public class DeploymentPlanImpl implements DeploymentPlan {

    private static final long serialVersionUID = -119621318892470668L;
    private final UUID uuid = UUID.randomUUID();
    private final List<DeploymentActionImpl> deploymentActions = new ArrayList<DeploymentActionImpl>();
    private final boolean globalRollback;
    private final boolean shutdown;
    private final long gracefulShutdownPeriod;

    DeploymentPlanImpl(List<DeploymentActionImpl> actions, boolean globalRollback, boolean shutdown, long gracefulTimeout) {
        if (actions == null)
            throw MESSAGES.nullVar("actions");
        this.deploymentActions.addAll(actions);
        this.globalRollback = globalRollback;
        this.shutdown = shutdown;
        this.gracefulShutdownPeriod = gracefulTimeout;
    }

    @Override
    public UUID getId() {
        return uuid;
    }

    @Override
    public List<DeploymentAction> getDeploymentActions() {
        return new ArrayList<DeploymentAction>(deploymentActions);
    }

    @Override
    public boolean isGlobalRollback() {
        return globalRollback;
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

    /**
     * Same as {@link #getDeploymentActions()} except the type of the list
     * contents reflects the actual implementation class.
     *
     * @return  the actions. Will not be <code>null</code>
     */
    public List<DeploymentActionImpl> getDeploymentActionImpls() {
        return new ArrayList<DeploymentActionImpl>(deploymentActions);
    }

    void cleanup() {
        for (DeploymentActionImpl action : deploymentActions) {
            if (action.isInternalStream() && action.getContentStream() != null) {
                StreamUtils.safeClose(action.getContentStream());
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        cleanup();
    }
}
