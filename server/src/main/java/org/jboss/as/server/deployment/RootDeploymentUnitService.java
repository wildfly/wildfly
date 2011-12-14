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

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.repository.api.ServerDeploymentRepository;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VirtualFile;

/**
 * The top-level service corresponding to a deployment unit.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class RootDeploymentUnitService extends AbstractDeploymentUnitService {
    private final InjectedValue<ServerDeploymentRepository> serverDeploymentRepositoryInjector = new InjectedValue<ServerDeploymentRepository>();
    private final String name;
    private final String managementName;
    final InjectedValue<VirtualFile> contentsInjector = new InjectedValue<VirtualFile>();
    private final DeploymentUnit parent;
    private final ImmutableManagementResourceRegistration registration;
    private final ManagementResourceRegistration mutableRegistration;
    private final ServiceVerificationHandler serviceVerificationHandler;
    private Resource resource;

    /**
     * Construct a new instance.
     *
     * @param name the deployment unit simple name
     * @param managementName the deployment's domain-wide unique name
     * @param parent the parent deployment unit
     * @param registration the registration
     * @param mutableRegistration the mutable registration
     * @param resource the model
     * @param serviceVerificationHandler
     */
    public RootDeploymentUnitService(final String name, final String managementName, final DeploymentUnit parent, final ImmutableManagementResourceRegistration registration, final ManagementResourceRegistration mutableRegistration, Resource resource, final ServiceVerificationHandler serviceVerificationHandler) {
        this.serviceVerificationHandler = serviceVerificationHandler;
        assert name != null : "name is null";
        this.name = name;
        this.managementName = managementName;
        this.parent = parent;
        this.registration = registration;
        this.mutableRegistration = mutableRegistration;
        this.resource = resource;
    }

    protected DeploymentUnit createAndInitializeDeploymentUnit(final ServiceRegistry registry) {
        final DeploymentUnit deploymentUnit = new DeploymentUnitImpl(parent, name, registry);
        deploymentUnit.putAttachment(Attachments.RUNTIME_NAME, name);
        deploymentUnit.putAttachment(Attachments.MANAGEMENT_NAME, managementName);
        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_CONTENTS, contentsInjector.getValue());
        deploymentUnit.putAttachment(DeploymentModelUtils.REGISTRATION_ATTACHMENT, registration);
        deploymentUnit.putAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT, mutableRegistration);
        deploymentUnit.putAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE, resource);
        deploymentUnit.putAttachment(Attachments.SERVICE_VERIFICATION_HANDLER, serviceVerificationHandler);

        // Attach the deployment repo
        deploymentUnit.putAttachment(Attachments.SERVER_DEPLOYMENT_REPOSITORY, serverDeploymentRepositoryInjector.getValue());

        return deploymentUnit;
    }

    Injector<ServerDeploymentRepository> getServerDeploymentRepositoryInjector() {
        return serverDeploymentRepositoryInjector;
    }
}
