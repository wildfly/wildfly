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

import java.util.concurrent.TimeUnit;


/**
 * Variant of a {@link DeploymentPlanBuilder} that is meant
 * to be used at the initial stages of the building process, when directives that
 * pertain to the entire plan can be applied.
 *
 * @author Brian Stansberry
 */
public interface InitialDeploymentPlanBuilder extends InitialDeploymentSetBuilder {

    /**
     * Indicates that the actions in the plan need to be rolled back across any single
     * given server group, then it should be rolled back across all server groups.
     *
     * @return a builder that can continue building the overall deployment plan
     */
    InitialDeploymentSetBuilder withRollbackAcrossGroups();

    /**
     * Indicates that on a given server all <code>deploy</code>, <code>undeploy</code> or
     * <code>replace</code> operations associated with the deployment set
     * should be rolled back in case of a failure in any of them.
     * <p>
     * <strong>Note:</strong> This directive does not span across servers, i.e.
     * a rollback on one server will not trigger rollback on others. Use
     * {@link ServerGroupDeploymentPlanBuilder#withRollback()} to trigger
     * rollback across servers.
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @deprecated single server rollback is the default behavior for a deployment plan and doesn't need to be enabled
     */
    @Deprecated
    @Override
    InitialDeploymentSetBuilder withSingleServerRollback();

    /**
     * Indicates that on a given server all <code>deploy</code>, <code>undeploy</code> or
     * <code>replace</code> operations associated with the deployment set
     * should <strong>not</strong> be rolled back in case of a failure in any of them.
     *
     * @return a builder that can continue building the overall deployment plan
     */
    @Override
    InitialDeploymentSetBuilder withoutSingleServerRollback();

    /**
     * Indicates actions specified subsequent to this call should be organized
     * around a full graceful server shutdown and restart. The server will attempt to shut itself
     * down gracefully, waiting for in-process work to complete before shutting
     * down. See the full JBoss AS documentation for details on what "waiting for
     * in-process work to complete" means.
     *
     * <p>For any <code>deploy</code> or <code>replace</code>
     * actions, the new content will not be deployed until the server is restarted.
     * For any <code>undeploy</code> or <code>replace</code> actions, the old content
     * will be undeployed as part of normal server shutdown processing.</p>
     *
     * @param timeout maximum amount of time the graceful shutdown should wait for
     *                existing work to complete before completing the shutdown
     * @param timeUnit {@link TimeUnit} in which <code>timeout</code> is expressed
     * @return a builder that can continue building the overall deployment plan
     */
    @Override
    InitialDeploymentSetBuilder withGracefulShutdown(long timeout, TimeUnit timeUnit);

    /**
     * Indicates actions specified subsequent to this call should be organized
     * around a full server restart. For any <code>deploy</code> or <code>replace</code>
     * actions, the new content will not be deployed until the server is restarted.
     * For any <code>undeploy</code> or <code>replace</code> actions, the old content
     * will be undeployed as part of normal server shutdown processing.
     *
     * @return a builder that can continue building the overall deployment plan
     */
    @Override
    InitialDeploymentSetBuilder withShutdown();

}
