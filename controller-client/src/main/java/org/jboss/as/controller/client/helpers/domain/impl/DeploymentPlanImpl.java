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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.controller.client.DeploymentMetadata;
import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlan;


/**
 * Describes a set of actions to take to change the deployment content available
 * to deployed in a server group or set of server groups.
 *
 * @author Brian Stansberry
 */
public class DeploymentPlanImpl implements DeploymentPlan, Serializable {

    private static final long serialVersionUID = -7652253540766375101L;

    private final DeploymentSetPlanImpl delegate;
    private final boolean rollbackAcrossGroups;

    DeploymentPlanImpl(DeploymentSetPlanImpl delegate,final boolean rollbackAcrossGroups) {
        this.delegate = delegate;
        this.rollbackAcrossGroups = rollbackAcrossGroups;
    }

    @Override
    public UUID getId() {
        return delegate.getId();
    }

    public DeploymentAction getLastAction() {
        return delegate.getLastAction();
    }

    @Override
    public List<DeploymentAction> getDeploymentActions() {
        return delegate.getDeploymentActions();
    }

    @Override
    public boolean isSingleServerRollback() {
        return delegate.isRollback();
    }

    @Override
    public boolean isRollbackAcrossGroups() {
        return rollbackAcrossGroups;
    }

    @Override
    public long getGracefulShutdownTimeout() {
        return delegate.getGracefulShutdownTimeout();
    }

    @Override
    public boolean isGracefulShutdown() {
        return delegate.isGracefulShutdown();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public DeploymentMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public List<Set<ServerGroupDeploymentPlan>> getServerGroupDeploymentPlans() {
        return delegate.getServerGroupDeploymentPlans();
    }

    List<DeploymentActionImpl> getDeploymentActionImpls() {
        List<DeploymentAction> actions = delegate.getDeploymentActions();
        List<DeploymentActionImpl> cast = new ArrayList<DeploymentActionImpl>(actions.size());
        for (DeploymentAction action : actions) {
            cast.add(DeploymentActionImpl.class.cast(action));
        }
        return cast;
    }
}
