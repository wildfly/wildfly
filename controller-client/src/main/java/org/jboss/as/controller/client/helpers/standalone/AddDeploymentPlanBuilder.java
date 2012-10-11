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
package org.jboss.as.controller.client.helpers.standalone;

import java.util.Map;

import org.jboss.as.controller.client.DeploymentMetadata;

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
     * <code>add</code> operation should be deployed, replacing the specified
     * existing content in the runtime.
     *
     * @param toReplace unique identifier of the existing deployment content that is to be replaced
     *
     * @return a builder that can continue building the overall deployment plan
     */
    ReplaceDeploymentPlanBuilder andReplace(String toReplace);

    /**
     * Add some user defined metadata to the {@link DeploymentPlan}.
     * See {@link DeploymentMetadata} for the set of supported types.
     *
     * @return a builder that can continue building the overall deployment plan
     */
    AddDeploymentPlanBuilder addMetadata(Map<String, Object> userdata);

    /**
     * Indicates that the specified deployment content should be deployed but not started.
     *
     * @return a builder that can continue building the overall deployment plan
     */
    AddDeploymentPlanBuilder andNoStart();
}
