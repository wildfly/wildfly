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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Builder capable of creating a {@link DeploymentPlan}. This interface
 * defines the core set of builder operations; various subinterfaces define
 * additional operations that become available as a result of executing the
 * methods in this interface.a
 *
 * @author Brian Stansberry
 */
public interface DeploymentPlanBuilder {

    /**
     * Gets the {@link DeploymentAction} most recently created as a result of
     * builder operations.
     *
     * @return the last action or <code>null</code> if there have been no actions.
     */
    DeploymentAction getLastAction();

    /**
     * Indicates the content of the specified file should be added to the default content
     * repository. The name of the deployment will be the value returned by
     * <code>{@link File#getName() file.getName()}</code>.
     * <p>
     * Note that this operation does not indicate the content should
     * be deployed into the runtime. See {@link #andDeploy()}.
     *
     * @param file file containing the new content
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @throws DuplicateDeploymentNameException if the name of the deployment is
     *              the same as that of other deployment content already present
     *              in the domain
     * @IOException if an exception occurs passing the deployment content to
     *              the domain
     */
    AddDeploymentPlanBuilder add(File file) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content at the specified URL should be added to the default content
     * repository.  The name of the deployment will be the last segment of the value returned by
     * <code>{@link URL#getPath() url.getPath()}</code>.
     * <p>
     * Note that this operation does not indicate the content should
     * be deployed into the runtime. See {@link #andDeploy()}.
     *
     * @param url URL pointing to the new content
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @throws DuplicateDeploymentNameException if the name of the deployment is
     *              the same as that of other deployment content already present
     *              in the domain
     * @IOException if an exception occurs passing the deployment content to
     *              the domain
     */
    AddDeploymentPlanBuilder add(URL url) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content of the specified fileL should be added to the default content
     * repository.
     * <p>
     * Note that this operation does not indicate the content should
     * be deployed into the runtime. See {@link #andDeploy()}.
     *
     * @param name name that should be given to the new content
     * @param file file containing the new content
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @throws DuplicateDeploymentNameException if the name of the deployment is
     *              the same as that of other deployment content already present
     *              in the domain
     * @IOException if an exception occurs passing the deployment content to
     *              the domain
     */
    AddDeploymentPlanBuilder add(String name, File file) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content at the specified URL should be added to the default content
     * repository.
     * <p>
     * Note that this operation does not indicate the content should
     * be deployed into the runtime. See {@link #andDeploy()}.
     *
     * @param name name that should be given to the new content
     * @param url URL pointing to the new content
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @throws DuplicateDeploymentNameException if the name of the deployment is
     *              the same as that of other deployment content already present
     *              in the domain
     * @IOException if an exception occurs passing the deployment content to
     *              the domain
     */
    AddDeploymentPlanBuilder add(String name, URL url) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content readable from the specified <code>InputStream</code>
     * should be added to the default content repository.
     * <p>
     * Note that this operation does not indicate the content should
     * be deployed into the runtime. See {@link #andDeploy()}.
     *
     * @param name name that should be given to the new content
     * @param stream <code>InputStream</code> from which the new content should be read
     * This stream has to be closed by the caller.
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @throws DuplicateDeploymentNameException if the name of the deployment is
     *              the same as that of other deployment content already present
     *              in the domain
     * @IOException if an exception occurs passing the deployment content to
     *              the domain
     */
    AddDeploymentPlanBuilder add(String name, InputStream stream) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content readable from the specified <code>InputStream</code>
     * should be added to the content repository.
     * <p>
     * Note that this operation does not indicate the content should
     * be deployed into the runtime. See {@link #andDeploy()}.
     *
     * @param name name that should be given to the new content to uniquely
     *             identify it within the domain's management system. Must be different from the
     *             name given to an other deployment content presently available
     *             on the server
     * @param commonName name by which the deployment should be known within
     *                   a server's runtime. This would be equivalent to the file name
     *                   of a deployment file, and would form the basis for such
     *                   things as default Java Enterprise Edition application and
     *                   module names. This would typically be the same
     *                   as {@code name} (in which case {@link #add(String, InputStream)}
     *                   would normally be used, but in some cases users may wish
     *                   to have two deployments with the same common name may (e.g.
     *                   two versions of "foo.war" both available in the deployment
     *                   content repository), in which case the deployments
     *                   would need to have distinct {@code name} values but
     *                   would have the same {@code commonName}
     * @param stream <code>InputStream</code> from which the new content should be read
     * This stream has to be closed by the caller.
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @throws DuplicateDeploymentNameException if the {@code name} of the deployment is
     *              the same as that of other deployment content already present
     *              in the domain
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    AddDeploymentPlanBuilder add(String name, String commonName, InputStream stream) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates a deployment that has already been added to the deployment
     * content repository should be added to the server groups associated with
     * the current {@link DeploymentSetPlan}. Typically this is used to add
     * content that was added as part of a previously executed plan to one or more
     * new server groups.
     *
     * @param name name that uniquely identifies the deployment within
     *             the domain's management system.
     * @throws IOException
     */
    AddDeploymentPlanBuilder add(String uniqueName) throws IOException;

    /**
     * Indicates the specified deployment content should be deployed.
     *
     * @param deploymentName unique identifier of the deployment content
     *
     * @return a builder that can continue building the overall deployment plan
     */
    DeployDeploymentPlanBuilder deploy(String deploymentName);

    /**
     * Indicates the specified deployment content should be undeployed.
     *
     * @param deploymentName unique identifier of the deployment content
     *
     * @return a builder that can continue building the overall deployment plan
     */
    UndeployDeploymentPlanBuilder undeploy(String deploymentName);

    /**
     * Indicates the specified deployment content should be redeployed (i.e.
     * undeployed and then deployed again).
     *
     * @param deploymentName unique identifier of the deployment content
     *
     * @return a builder that can continue building the overall deployment plan
     */
    DeploymentPlanBuilder redeploy(String deploymentName);

    /**
     * Indicates the specified deployment content should be deployed, replacing
     * the specified existing deployment.
     *
     * @param replacement unique identifier of the content to deploy
     * @param toReplace unique identifier of the currently deployed content to replace
     *
     * @return a builder that can continue building the overall deployment plan
     */
    ReplaceDeploymentPlanBuilder replace(String replacementDeploymentName, String toReplaceDeploymentName);

    /**
     * Indicates the content of the specified file should be added to the content
     * repository and replace existing content of the same name. The name of the
     * deployment will be the value returned by
     * <code>{@link File#getName() file.getName()}</code>.
     * <p>
     * Whether this operation will result in the new content being deployed into
     * the runtime depends on whether the existing content being replaced is
     * deployed. If the content being replaced is deployed the old content will
     * be undeployed and the new content will be deployed.</p>
     *
     * @param file file containing the new content
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    RemoveDeploymentPlanBuilder replace(File file) throws IOException;

    /**
     * Indicates the content at the specified URL should be added to the content
     * repository and replace existing content of the same name.  The name of
     * the deployment will be the last segment of the value returned by
     * <code>{@link URL#getPath() url.getPath()}</code>.
     * <p>
     * Whether this operation will result in the new content being deployed into
     * the runtime depends on whether the existing content being replaced is
     * deployed. If the content being replaced is deployed the old content will
     * be undeployed and the new content will be deployed.</p>
     *
     * @param url URL pointing to the new content
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    RemoveDeploymentPlanBuilder replace(URL url) throws IOException;

    /**
     * Indicates the content of the specified file should be added to the content
     * repository and replace existing content of the same name.
     * <p>
     * Whether this operation will result in the new content being deployed into
     * the runtime depends on whether the existing content being replaced is
     * deployed. If the content being replaced is deployed the old content will
     * be undeployed and the new content will be deployed.</p>
     *
     * @param name name that should be given to the new content
     * @param file file containing the new content
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    RemoveDeploymentPlanBuilder replace(String name, File file) throws IOException;

    /**
     * Indicates the content at the specified URL should be added to the content
     * repository and replace existing content of the same name.
     * <p>
     * Whether this operation will result in the new content being deployed into
     * the runtime depends on whether the existing content being replaced is
     * deployed. If the content being replaced is deployed the old content will
     * be undeployed and the new content will be deployed.</p>
     *
     * @param name name that should be given to the new content
     * @param url URL pointing to the new content
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    RemoveDeploymentPlanBuilder replace(String name, URL url) throws IOException;

    /**
     * Indicates the content readable from the specified <code>InputStream</code>
     * should be added to the content repository and replace existing
     * content of the same name.
     * <p>
     * Whether this operation will result in the new content being deployed into
     * the runtime depends on whether the existing content being replaced is
     * deployed. If the content being replaced is deployed the old content will
     * be undeployed and the new content will be deployed.</p>
     *
     * @param name name that should be given to the new content
     * @param stream <code>InputStream</code> from which the new content should be read
     * This stream has to be closed by the caller.
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @throws DuplicateDeploymentNameException if the name of the deployment is
     *              the same as that of other deployment content already present
     *              on the server
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    RemoveDeploymentPlanBuilder replace(String name, InputStream stream) throws IOException;

    /**
     * Indicates the content readable from the specified <code>InputStream</code>
     * should be added to the content repository and replace existing
     * content of the same name.
     * <p>
     * Whether this operation will result in the new content being deployed into
     * the runtime depends on whether the existing content being replaced is
     * deployed. If the content being replaced is deployed the old content will
     * be undeployed and the new content will be deployed.</p>
     *
     * @param name name that should be given to the new content
     * @param commonName name by which the deployment should be known within
     *                   the runtime. This would be equivalent to the file name
     *                   of a deployment file, and would form the basis for such
     *                   things as default Java Enterprise Edition application and
     *                   module names. This would typically be the same
     *                   as {@code name} (in which case {@link #add(String, InputStream)}
     *                   would normally be used, but in some cases users may wish
     *                   to have two deployments with the same common name may (e.g.
     *                   two versions of "foo.war" both available in the deployment
     *                   content repository), in which case the deployments
     *                   would need to have distinct {@code name} values but
     *                   would have the same {@code commonName}
     * @param stream <code>InputStream</code> from which the new content should be read
     * This stream has to be closed by the caller.
     *
     * @return a builder that can continue building the overall deployment plan
     *
     * @throws DuplicateDeploymentNameException if the name of the deployment is
     *              the same as that of other deployment content already present
     *              on the server
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    RemoveDeploymentPlanBuilder replace(String name, String commonName, InputStream stream) throws IOException;

    /**
     * Indicates the specified deployment content should be removed from the
     * content repository.
     *
     * @param deploymentName unique identifier of the deployment content
     *
     * @return a builder that can continue building the overall deployment plan
     */
    RemoveDeploymentPlanBuilder remove(String deploymentName);

    /**
     * Creates the deployment plan.
     *
     * @return the deployment plan
     */
    DeploymentPlan build();

}
