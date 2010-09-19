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

import java.util.concurrent.TimeUnit;

import org.jboss.as.deployment.client.api.domain.InitialDeploymentSetBuilder;
import org.jboss.as.deployment.client.impl.DeploymentActionImpl;
import org.jboss.as.deployment.client.impl.DeploymentContentDistributor;

/**
 * Variant of a {@link DeploymentPlanBuilderImpl} that is meant
 * to be used at the initial stages of the building process, when directives that
 * pertain to the entire plan can be applied.
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

    InitialDeploymentSetBuilderImpl(DeploymentPlanBuilderImpl existing) {
        super(existing);
    }

    InitialDeploymentSetBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentActionImpl modification) {
        super(existing, modification);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.domain.InitialDeploymentPlanBuilder#withGracefulShutdown(long, java.util.concurrent.TimeUnit)
     */
    public InitialDeploymentSetBuilder withGracefulShutdown(long timeout, TimeUnit timeUnit) {

        long period = timeUnit.toMillis(timeout);
        if (restart && period != gracefulShutdownPeriod) {
            throw new IllegalStateException("Graceful shutdown already configured with a timeout of " + gracefulShutdownPeriod + " ms");
        }
        this.restart = true;
        this.gracefulShutdownPeriod = period;
        return new InitialDeploymentSetBuilderImpl(this);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.domain.InitialDeploymentPlanBuilder#withShutdown()
     */
    public InitialDeploymentSetBuilder withShutdown() {

        this.restart = true;
        return new InitialDeploymentSetBuilderImpl(this);
    }

    @Override
    public InitialDeploymentSetBuilder withDeploymentSetRollback() {
        // FIXME implement this
        throw new UnsupportedOperationException("implement me");
    }
}
