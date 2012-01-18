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
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Service responsible for installing the correct services to install a {@link DeploymentUnit}.
 *
 * @author John Bailey
 */
public class SubDeploymentUnitService extends AbstractDeploymentUnitService {
    private final ResourceRoot deploymentRoot;
    private final DeploymentUnit parent;
    private final ImmutableManagementResourceRegistration registration;
    private final ServiceVerificationHandler serviceVerificationHandler;
    private Resource resource;

    public SubDeploymentUnitService(ResourceRoot deploymentRoot, DeploymentUnit parent, ImmutableManagementResourceRegistration registration, Resource resource, final ServiceVerificationHandler serviceVerificationHandler) {
        this.serviceVerificationHandler = serviceVerificationHandler;
        if (deploymentRoot == null) throw ServerMessages.MESSAGES.deploymentRootRequired();
        this.deploymentRoot = deploymentRoot;
        if (parent == null) throw ServerMessages.MESSAGES.subdeploymentsRequireParent();
        this.parent = parent;
        this.registration = registration;
        this.resource = resource;
    }

    protected DeploymentUnit createAndInitializeDeploymentUnit(ServiceRegistry registry) {
        final String deploymentName = deploymentRoot.getRootName();
        final DeploymentUnit deploymentUnit = new DeploymentUnitImpl(parent, deploymentName, registry);
        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_ROOT, deploymentRoot);
        deploymentUnit.putAttachment(Attachments.MODULE_SPECIFICATION, new ModuleSpecification());
        deploymentUnit.putAttachment(DeploymentModelUtils.REGISTRATION_ATTACHMENT, registration);
        deploymentUnit.putAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE, resource);
        deploymentUnit.putAttachment(Attachments.SERVICE_VERIFICATION_HANDLER, serviceVerificationHandler);
        this.resource = null;
        return deploymentUnit;
    }

}
