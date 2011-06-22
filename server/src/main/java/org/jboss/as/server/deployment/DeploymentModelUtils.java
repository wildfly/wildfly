/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class DeploymentModelUtils {

    static final AttachmentKey<DeploymentModelUtils> KEY = AttachmentKey.create(DeploymentModelUtils.class);
    static final String SUBSYSTEM = ModelDescriptionConstants.SUBSYSTEM;
    static final String SUB_DEPLOYMENT = "subdeployment";

    private final Resource root;
    private final ImmutableManagementResourceRegistration registration;

    DeploymentModelUtils(final Resource root, final ImmutableManagementResourceRegistration registration) {
        this.root = root;
        this.registration = registration;
    }

    void initialize() {
        for(final Resource.ResourceEntry entry : root.getChildren(SUBSYSTEM)) {
            root.removeChild(entry.getPathElement());
        }
        for(final Resource.ResourceEntry entry : root.getChildren(SUB_DEPLOYMENT)) {
            root.removeChild(entry.getPathElement());
        }
    }

    void cleanup() {
        for(final Resource.ResourceEntry entry : root.getChildren(SUBSYSTEM)) {
            root.removeChild(entry.getPathElement());
        }
        for(final Resource.ResourceEntry entry : root.getChildren(SUB_DEPLOYMENT)) {
            root.removeChild(entry.getPathElement());
        }
    }

    ModelNode createDeploymentSubModel(final String subsystemName, final PathElement address) {
        final Resource subsystem = getOrCreate(root, PathElement.pathElement(SUBSYSTEM, subsystemName));
        final ImmutableManagementResourceRegistration subModel = registration.getSubModel(getExtensionAddress(subsystemName, address));
        if(subModel == null) {
            throw new IllegalStateException(address.toString());
        }
        return getOrCreate(subsystem, address).getModel();
    }

    DeploymentModelUtils createSubDeployment(final String deploymentName) {
        final Resource subDeploymentRoot = getOrCreate(root, PathElement.pathElement(SUB_DEPLOYMENT,deploymentName));
        final DeploymentModelUtils utils = new DeploymentModelUtils(subDeploymentRoot, registration);
        utils.initialize();
        return utils;
    }

    static Resource getOrCreate(final Resource parent, final PathElement element) {
        if(parent.hasChild(element)) {
            return parent.requireChild(element);
        } else {
            final Resource resource = Resource.Factory.create();
            parent.registerChild(element, resource);
            return resource;
        }
    }

    static PathAddress getExtensionAddress(final String subsystemName, final PathElement element) {
        return PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystemName), element);
    }

    static DeploymentModelUtils create(final NewOperationContext context, final PathAddress address) {
            final Resource resource = context.readResourceForUpdate(address);

            final ImmutableManagementResourceRegistration registration = context.getResourceRegistration().getSubModel(address);
            return new DeploymentModelUtils(resource, registration);
    }

}
