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

package org.jboss.as.controller.client.helpers.domain;

/**
 * Variant of a {@link DeploymentPlanBuilder} that exposes
 * directives that are only applicable when controlling how to limit
 * {@link ServerGroupDeploymentPlanBuilder#withRollback() rollbacks} when a
 * {@link DeploymentSetPlan} is applied to a server groups.
 *
 * @author Brian Stansberry
 */
public interface RollbackDeploymentPlanBuilder extends ServerGroupDeploymentPlanBuilder {

    /**
     * Allows the application of the deployment set to fail on the given
     * number of servers before triggering rollback of the plan.
     *
     * @param serverFailures the number of servers. Must be greater than <code>0</code>
     *
     * @return a builder that can continue building the overall deployment plan
     */
    ServerGroupDeploymentPlanBuilder allowFailures(int serverFailures);

    /**
     * Allows the application of the deployment set to fail on the given
     * percentage of servers in the server group before triggering rollback of the plan.
     *
     * @param serverFailurePercentage the percentage of servers. Must be between
     *              <code>1</code> and <code>99</code>
     *
     * @return a builder that can continue building the overall deployment plan
     */
    ServerGroupDeploymentPlanBuilder allowPercentageFailures(int serverFailurePercentage);

}
