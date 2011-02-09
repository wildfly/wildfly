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

package org.jboss.as.server.deployment;

import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.value.InjectedValue;

/**
 * The top-level service corresponding to a deployment unit.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RootDeploymentUnitService extends AbstractDeploymentUnitService {
    private final InjectedValue<ServerDeploymentRepository> serverDeploymentRepositoryInjector = new InjectedValue<ServerDeploymentRepository>();
    private final String name;
    private final String runtimeName;
    private final byte[] deploymentHash;
    private final DeploymentUnit parent;

    /**
     * Construct a new instance.
     *
     * @param name the deployment unit simple name
     * @param runtimeName the deployment runtime name
     * @param deploymentHash the deployment hash
     * @param parent the parent deployment unit
     */
    public RootDeploymentUnitService(final String name, final String runtimeName, final byte[] deploymentHash, final DeploymentUnit parent) {
        this.name = name;
        this.parent = parent;
        this.runtimeName = runtimeName;
        this.deploymentHash = deploymentHash;
    }

    protected DeploymentUnit createAndInitializeDeploymentUnit(final ServiceRegistry registry) {
        final DeploymentUnit deploymentUnit = new DeploymentUnitImpl(parent, name, registry);
        deploymentUnit.putAttachment(Attachments.RUNTIME_NAME, runtimeName);
        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_HASH, deploymentHash);

        // Attach the deployment repo
        deploymentUnit.putAttachment(Attachments.SERVER_DEPLOYMENT_REPOSITORY, serverDeploymentRepositoryInjector.getValue());

        return deploymentUnit;
    }

    Injector<ServerDeploymentRepository> getServerDeploymentRepositoryInjector() {
        return serverDeploymentRepositoryInjector;
    }
}
