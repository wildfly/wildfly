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
 * directives that signal indicate the completion of the phase of establishing
 * the set of {@link DeploymentAction}s that comprise a {@link DeploymentSetPlan}
 * and the beginning of the phase of specifying how those actions should be
 * applied to server groups.
 *
 * @author Brian Stansberry
 */
public interface DeploymentActionsCompleteBuilder extends DeploymentPlanBuilder {

    /**
     * Indicates that the current set of {@link DeploymentAction deployment actions} comprise
     * a {@link DeploymentSetPlan} and should be applied to a server group.
     * Once this method is invoked, no further actions will be included in the
     * <code>DeploymentSetPlan</code>.
     * <p>
     * Any subsequent <code>add</code>, <code>remove</code>, <code>deploy</code>,
     * <code>replace</code> or <code>undeploy</code> builder operations will
     * signal the start of a new <code>DeploymentSetPlan</code>.
     * </p>
     *
     * @param serverGroupName the name of the server group. Cannot be <code>null</code>
     *
     * @return a builder that can continue building the overall deployment plan
     */
    ServerGroupDeploymentPlanBuilder toServerGroup(String serverGroupName);

}
