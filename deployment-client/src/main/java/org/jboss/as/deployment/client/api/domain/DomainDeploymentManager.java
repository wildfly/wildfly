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

package org.jboss.as.deployment.client.api.domain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jboss.as.deployment.client.api.server.DeploymentPlan;

/**
 * Primary deployment interface for a JBoss AS Domain Controller.
 *
 * @author Brian Stansberry
 */
public interface DomainDeploymentManager {

    /**
     * Indicates the content of the specified file should be added to the domain's deployment content
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
     */
    String addDeploymentContent(File file) throws IOException;

    /**
     * Indicates the content at the specified URL should be added to the domain's deployment content
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
     */
    String addDeploymentContent(URL url) throws IOException;

    /**
     * Indicates the content of the specified file should be added to the domain's deployment content
     * repository.
     * <p>
     * Note that this operation does not result in the content being deployed
     * into the runtime; for that a {@link DeploymentPlan} needs to be
     * {@link #newDeploymentPlan() created} and {@link #execute(DeploymentPlan) executed}.
     * </p>
     *
     * @param name unique name that should be given to the new content. Must be
     *             different from the name of any other content currently
     *             in the domain's deployment content repository
     * @param file file containing the new content
     */
    void addDeploymentContent(String name, File file) throws IOException;

    /**
     * Indicates the content at the specified URL should be added to the default content
     * repository.
     * <p>
     * Note that this operation does not result in the content being deployed
     * into the runtime; for that a {@link DeploymentPlan} needs to be
     * {@link #newDeploymentPlan() created} and {@link #execute(DeploymentPlan) executed}.
     * </p>
     *
     * @param name unique name that should be given to the new content. Must be
     *             different from the name of any other content currently
     *             in the domain's deployment content repository
     * @param url URL pointing to the new content
     */
    void addDeploymentContent(String name, URL url) throws IOException;

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
     */
    void addDeploymentContent(String name, InputStream stream) throws IOException;

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
     * @return the results of the deployment plan
     */
    DeploymentPlanResult execute(DeploymentPlan plan);
}
