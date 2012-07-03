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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.controller.client.DeploymentMetadata;
import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.DeploymentSetPlan;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlan;


/**
 * TODO get rid of this class and put all logic in DeploymentPlanImpl
 *
 * @author Brian Stansberry
 */
public class DeploymentSetPlanImpl implements DeploymentSetPlan, Serializable {

    private static final long serialVersionUID = -7652253540766375101L;

    private final UUID uuid;
    private final List<DeploymentAction> deploymentActions = new ArrayList<DeploymentAction>();
    private final DeploymentMetadata metadata;
    private final boolean rollback;
    private final boolean shutdown;
    private final long gracefulShutdownPeriod;
    private final List<Set<ServerGroupDeploymentPlan>> serverGroupPlans = new ArrayList<Set<ServerGroupDeploymentPlan>>();

    DeploymentSetPlanImpl() {
        this.uuid = UUID.randomUUID();
        this.rollback = true;
        this.shutdown = false;
        this.gracefulShutdownPeriod = -1;
        this.metadata = DeploymentMetadata.UNDEFINED;
        this.serverGroupPlans.add(new LinkedHashSet<ServerGroupDeploymentPlan>());
    }

    private DeploymentSetPlanImpl(final UUID uuid,
            final List<DeploymentAction> actions,
            final List<Set<ServerGroupDeploymentPlan>> serverGroupPlans,
            final DeploymentMetadata metadata,
            final boolean rollback,
            final boolean shutdown,
            final long gracefulTimeout) {
        this.uuid = uuid;
        this.deploymentActions.addAll(actions);
        this.metadata = metadata;
        this.rollback = rollback;
        this.shutdown = shutdown;
        this.gracefulShutdownPeriod = gracefulTimeout;
        this.serverGroupPlans.addAll(serverGroupPlans);
        LinkedHashSet<ServerGroupDeploymentPlan> last = (LinkedHashSet<ServerGroupDeploymentPlan>) serverGroupPlans.get(serverGroupPlans.size() -1);
        this.serverGroupPlans.set(serverGroupPlans.size() - 1, new LinkedHashSet<ServerGroupDeploymentPlan>(last));
    }

    @Override
    public UUID getId() {
        return uuid;
    }

    public DeploymentAction getLastAction() {
        return deploymentActions.size() == 0 ? null : deploymentActions.get(deploymentActions.size() - 1);
    }

    @Override
    public List<DeploymentAction> getDeploymentActions() {
        return new ArrayList<DeploymentAction>(deploymentActions);
    }

    @Override
    public boolean isRollback() {
        return rollback;
    }

    @Override
    public DeploymentMetadata getMetadata() {
        return metadata;
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

    @Override
    public List<Set<ServerGroupDeploymentPlan>> getServerGroupDeploymentPlans() {
        List<Set<ServerGroupDeploymentPlan>> copy = null;
        if (serverGroupPlans != null) {
            copy = new ArrayList<Set<ServerGroupDeploymentPlan>>(serverGroupPlans.size());
            for (Set<ServerGroupDeploymentPlan> set : serverGroupPlans) {
                copy.add(Collections.unmodifiableSet(new LinkedHashSet<ServerGroupDeploymentPlan>(set)));
            }
        }
        else {
            copy = Collections.emptyList();
        }
        return Collections.unmodifiableList(copy);
    }

    boolean hasServerGroupPlans() {
        return serverGroupPlans.size() > 1 || serverGroupPlans.get(0).size() > 0;
    }

    ServerGroupDeploymentPlan getLatestServerGroupDeploymentPlan() {
        LinkedHashSet<ServerGroupDeploymentPlan> lastSet = (LinkedHashSet<ServerGroupDeploymentPlan>) serverGroupPlans.get(serverGroupPlans.size() -1);
        ServerGroupDeploymentPlan last = null;
        for (ServerGroupDeploymentPlan plan : lastSet) {
            last = plan;
        }
        return last;
    }

    DeploymentSetPlanImpl addAction(final DeploymentAction action) {
        DeploymentSetPlanImpl result = new DeploymentSetPlanImpl(this.uuid, this.deploymentActions, this.serverGroupPlans, this.metadata, this.rollback, this.shutdown, this.gracefulShutdownPeriod);
        result.deploymentActions.add(action);
        return result;
    }

    DeploymentSetPlanImpl addMetadata(final Map<String, Object> userdata) {
        DeploymentSetPlanImpl result = new DeploymentSetPlanImpl(this.uuid, this.deploymentActions, this.serverGroupPlans, new DeploymentMetadata(userdata), this.rollback, this.shutdown, this.gracefulShutdownPeriod);
        return result;
    }

    DeploymentSetPlanImpl setRollback() {
        DeploymentSetPlanImpl result = new DeploymentSetPlanImpl(this.uuid, this.deploymentActions, this.serverGroupPlans, this.metadata, true, this.shutdown, this.gracefulShutdownPeriod);
        return result;
    }

    DeploymentSetPlanImpl setNoRollback() {
        DeploymentSetPlanImpl result = new DeploymentSetPlanImpl(this.uuid, this.deploymentActions, this.serverGroupPlans, this.metadata, false, this.shutdown, this.gracefulShutdownPeriod);
        return result;
    }

    DeploymentSetPlanImpl setShutdown() {
        DeploymentSetPlanImpl result = new DeploymentSetPlanImpl(this.uuid, this.deploymentActions, this.serverGroupPlans, this.metadata, this.rollback, true, -1);
        return result;
    }

    DeploymentSetPlanImpl setGracefulTimeout(long timeout) {
        DeploymentSetPlanImpl result = new DeploymentSetPlanImpl(this.uuid, this.deploymentActions, this.serverGroupPlans, this.metadata, this.rollback, this.shutdown, timeout);
        return result;
    }

    DeploymentSetPlanImpl storeServerGroup(final ServerGroupDeploymentPlan groupPlan) {
        DeploymentSetPlanImpl result = new DeploymentSetPlanImpl(this.uuid, this.deploymentActions, this.serverGroupPlans, this.metadata, this.rollback, this.shutdown, this.gracefulShutdownPeriod);
        Set<ServerGroupDeploymentPlan> set = result.serverGroupPlans.get(result.serverGroupPlans.size() - 1);
        set.remove(groupPlan);
        set.add(groupPlan);
        return result;
    }

    DeploymentSetPlanImpl storeRollToServerGroup(final ServerGroupDeploymentPlan groupPlan) {
        DeploymentSetPlanImpl result = new DeploymentSetPlanImpl(this.uuid, this.deploymentActions, this.serverGroupPlans, this.metadata, this.rollback, this.shutdown, this.gracefulShutdownPeriod);
        Set<ServerGroupDeploymentPlan> set = result.serverGroupPlans.get(result.serverGroupPlans.size() - 1);
        result.serverGroupPlans.set(result.serverGroupPlans.size() - 1, Collections.unmodifiableSet(set));
        set = new LinkedHashSet<ServerGroupDeploymentPlan>();
        set.add(groupPlan);
        result.serverGroupPlans.add(set);
        return result;
    }
}
