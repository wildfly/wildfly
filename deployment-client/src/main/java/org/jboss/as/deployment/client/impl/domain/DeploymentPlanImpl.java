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
import org.jboss.as.deployment.client.api.domain.DeploymentPlan;
import org.jboss.as.deployment.client.api.domain.DeploymentSetPlan;

/**
 * Describes a set of actions to take to change the deployment content available
 * to and/or deployed in a standalone server.
 *
 * @author Brian Stansberry
 */
public class DeploymentPlanImpl implements DeploymentPlan {

    private final UUID uuid = UUID.randomUUID();
    private final List<DeploymentAction> addedContent = new ArrayList<DeploymentAction>();
    private final List<DeploymentAction> removedContent = new ArrayList<DeploymentAction>();
    private final List<DeploymentSetPlan> deploymentSets = new ArrayList<DeploymentSetPlan>();
    private final boolean globalRollback;

    DeploymentPlanImpl(List<DeploymentAction> addedContent, List<DeploymentAction> removedContent,
            List<DeploymentSetPlanImpl> deploymentSets, boolean globalRollback) {
        if (addedContent == null)
            throw new IllegalArgumentException("addedContent is null");
        if (removedContent == null)
            throw new IllegalArgumentException("removedContent is null");
        if (deploymentSets == null)
            throw new IllegalArgumentException("deploymentSets is null");
        this.addedContent.addAll(addedContent);
        this.removedContent.addAll(removedContent);
        this.deploymentSets.addAll(deploymentSets);
        this.globalRollback = globalRollback;
    }


    @Override
    public UUID getId() {
        return uuid;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.domain.DeploymentPlan#getAddedContent()
     */
    public List<DeploymentAction> getAddedContent() {
        return new ArrayList<DeploymentAction>(addedContent);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.domain.DeploymentPlan#getRemovedContent()
     */
    public List<DeploymentAction> getRemovedContent() {
        return new ArrayList<DeploymentAction>(removedContent);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.domain.DeploymentPlan#isGlobalRollback()
     */
    public boolean isGlobalRollback() {
        return globalRollback;
    }


    @Override
    public List<DeploymentSetPlan> getDeploymentSetPlans() {
        return new ArrayList<DeploymentSetPlan>(deploymentSets);
    }
}
