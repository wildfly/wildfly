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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import org.jboss.as.deployment.client.api.domain.DeploymentActionResult;
import org.jboss.as.deployment.client.api.domain.DeploymentSetPlanResult;
import org.jboss.as.deployment.client.api.domain.ServerGroupDeploymentPlanResult;

/**
 * TODO add class javadoc for DeploymentSetPlanResultImpl.
 *
 * @author Brian Stansberry
 */
public class DeploymentSetPlanResultImpl implements DeploymentSetPlanResult {

    private final UUID id;

    public DeploymentSetPlanResultImpl(UUID id) {
        this.id = id;
    }

    @Override
    public Future<DeploymentActionResult> getDeploymentActionResult(UUID deploymentAction) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UUID getDeploymentSetId() {
        return id;
    }

    @Override
    public Map<String, ServerGroupDeploymentPlanResult> getServerGroupsResults() {
        // TODO Auto-generated method stub
        return null;
    }

}
