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
package org.jboss.as.deployment.client.api.server;

/**
 * Extension of {@link DeploymentPlanBuilder} that exposes
 * directives that are only applicable following an <code>add</code> directive.
 *
 * @author Brian Stansberry
 */
public interface AddDeploymentPlanBuilder extends DeploymentPlanBuilder {

    /**
     * Indicates content that was added via an immediately preceding
     * <code>add</code> operation should be deployed.
     *
     * @return a builder that can continue building the overall deployment plan
     */
    DeploymentPlanBuilder andDeploy();

    /**
     * Indicates content that was added via an immediately preceding
     * <code>add</code> operation should be deployed, replacing in the runtime
     * the existing content of the same name.
     * <p>
     * The process of finding an existing deployment is as follows:
     * <ol>
     *   <li>All existing deployments with the same name are found</li>
     *   <li>If more than one such deployment is found, and some such deployments
     *   are started while others are not, all non-started deployments are eliminated
     *   from consideration</li>
     *   <li>If more than one such deployment remains, and some are associated
     *   with the default content repository, all deployments not associated with
     *   the default content repository are eliminated</li>
     *   <li>If at this point a single deployment remains, its key is returned,
     *   otherwise an IllegalStateException is thrown.</li>
     * </ol>
     * </p>
     * <p>
     * Note that this operation does not indicate the existing content should
     * be removed from the repository. See {@link #andRemoveUndeployed()}.
     * </p>
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @throws IllegalStateException if either zero or more than one matching existing
     *    deployments is found
     */
    ReplaceDeploymentPlanBuilder andReplaceSame();

    /**
     * Indicates content that was added via an immediately preceding
     * <code>add</code> operation should be deployed, replacing the specified
     * existing content in the runtime.
     *
     * @param toReplace unique identifier of the existing deployment content that is to be replaced
     *
     * @return a builder that can continue building the overall deployment plan
     */
    ReplaceDeploymentPlanBuilder andReplace(String toReplace);

}
