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

package org.jboss.as.standalone.client.api.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceName;

/**
 * Primary deployment interface for a standalone JBoss AS instance.
 *
 * @author Brian Stansberry
 */
public interface ServerDeploymentManager {

    ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("deployment-manager").append("server");
    ServiceName SERVICE_NAME_LOCAL = SERVICE_NAME_BASE.append("local");

    /**
     * Indicates the content of the specified file should be added to the default content
     * repository. The name of the deployment will be the value returned by
     * <code>{@link File#getName() file.getName()}</code>.
     * <p>
     * Note that this operation does not result in the content being deployed
     * into the runtime; for that a {@link DeploymentPlan} needs to be
     * {@link #newDeploymentPlan() created} and {@link #execute(DeploymentPlan) executed}.
     * </p>
     *
     * @param file file containing the new content
     *
     * @return unique name that can be used to reference the added content, in this
     *         case the value returned by {@link File#getName() file.getName()}
     *
     * @throws DuplicateDeploymentNameException if the {@code name} of the deployment is
     *              the same as that of other deployment content already present
     *              on the server
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    String addDeploymentContent(File file) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content at the specified URL should be added to the default content
     * repository.  The name of the deployment will be the last segment of the value returned by
     * <code>{@link URL#getPath() url.getPath()}</code>.
     * <p>
     * Note that this operation does not result in the content being deployed
     * into the runtime; for that a {@link DeploymentPlan} needs to be
     * {@link #newDeploymentPlan() created} and {@link #execute(DeploymentPlan) executed}.
     * </p>
     *
     * @param url URL pointing to the new content
     *
     * @return unique name that can be used to reference the added content, in this
     *         case the last segment of the value returned by {@link URL#getPath() url.getPath()}
     *
     * @throws DuplicateDeploymentNameException if the {@code name} of the deployment is
     *              the same as that of other deployment content already present
     *              on the server
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    String addDeploymentContent(URL url) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content of the specified file should be added to the default content
     * repository.
     * <p>
     * Note that this operation does not result in the content being deployed
     * into the runtime; for that a {@link DeploymentPlan} needs to be
     * {@link #newDeploymentPlan() created} and {@link #execute(DeploymentPlan) executed}.
     * </p>
     *
     * @param name name that should be given to the new content
     * @param file file containing the new content
     *
     * @throws DuplicateDeploymentNameException if the {@code name} of the deployment is
     *              the same as that of other deployment content already present
     *              on the server
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    void addDeploymentContent(String name, File file) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content at the specified URL should be added to the default content
     * repository.
     * <p>
     * Note that this operation does not result in the content being deployed
     * into the runtime; for that a {@link DeploymentPlan} needs to be
     * {@link #newDeploymentPlan() created} and {@link #execute(DeploymentPlan) executed}.
     * </p>
     *
     * @param name name that should be given to the new content
     * @param url URL pointing to the new content
     *
     * @throws DuplicateDeploymentNameException if the {@code name} of the deployment is
     *              the same as that of other deployment content already present
     *              on the server
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    void addDeploymentContent(String name, URL url) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content readable from the specified <code>InputStream</code>
     * should be added to the default content repository.
     * <p>
     * Note that this operation does not result in the content being deployed
     * into the runtime; for that a {@link DeploymentPlan} needs to be
     * {@link #newDeploymentPlan() created} and {@link #execute(DeploymentPlan) executed}.
     * </p>
     *
     * @param name name that should be given to the new content
     * @param stream <code>InputStream</code> from which the new content should be read
     *
     * @throws DuplicateDeploymentNameException if the {@code name} of the deployment is
     *              the same as that of other deployment content already present
     *              on the server
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    void addDeploymentContent(String name, InputStream stream) throws IOException, DuplicateDeploymentNameException;

    /**
     * Indicates the content readable from the specified <code>InputStream</code>
     * should be added to the content repository.
     * <p>
     * Note that this operation does not indicate the content should
     * be deployed into the runtime. See {@link #andDeploy()}.
     *
     * @param name name that should be given to the new content to uniquely
     *             identify it within the server's management system. Must be different from the
     *             name given to an other deployment content presently available
     *             on the server
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
     *
     * @throws DuplicateDeploymentNameException if the {@code name} of the deployment is
     *              the same as that of other deployment content already present
     *              on the server
     * @IOException if an exception occurs passing the deployment content to
     *              the server
     */
    void addDeploymentContent(String name, String commonName, InputStream stream) throws IOException, DuplicateDeploymentNameException;

    /**
     * Initiates the creation of a new {@link DeploymentPlan}.
     *
     * @return builder object for the {@link DeploymentPlan}
     */
    InitialDeploymentPlanBuilder newDeploymentPlan();

    /**
     * Execute the deployment plan.
     *
     * @param plan the deployment plan
     *
     * @return {@link Future} from which the results of the deployment plan can
     *         be obtained
     */
    Future<ServerDeploymentPlanResult> execute(DeploymentPlan plan);
}
