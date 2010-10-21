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

package org.jboss.as.domain.client.impl.deployment;

import org.jboss.as.domain.client.api.deployment.InitialDeploymentPlanBuilder;
import org.jboss.as.domain.client.api.deployment.InitialDeploymentSetBuilder;

/**
 * Variant of a {@link DeploymentPlanBuilderImpl} that is meant
 * to be used at the initial stages of the building process, when directives that
 * pertain to the entire plan can be applied.
 *
 * @author Brian Stansberry
 */
class InitialDeploymentPlanBuilderImpl extends InitialDeploymentSetBuilderImpl implements InitialDeploymentPlanBuilder  {

    /**
     * Constructs a new InitialDeploymentPlanBuilder
     */
    InitialDeploymentPlanBuilderImpl(DeploymentContentDistributor deploymentDistributor) {
        super(deploymentDistributor);
    }

    private InitialDeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, boolean globalRollback) {
        super(existing, globalRollback);
    }

    public InitialDeploymentSetBuilder withGlobalRollback() {
        return new InitialDeploymentPlanBuilderImpl(this, true);
    }
}
