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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 * TODO:  make this package protected again instead of public
 */
public class DeploymentModelUtils {

    // TODO:  make this package protected again instead of public
    public static final AttachmentKey<Resource> DEPLOYMENT_RESOURCE = AttachmentKey.create(Resource.class);
    static final AttachmentKey<ImmutableManagementResourceRegistration> REGISTRATION_ATTACHMENT = AttachmentKey.create(ImmutableManagementResourceRegistration.class);
    public static final AttachmentKey<ManagementResourceRegistration> MUTABLE_REGISTRATION_ATTACHMENT = AttachmentKey.create(ManagementResourceRegistration.class);

    static final String SUBSYSTEM = ModelDescriptionConstants.SUBSYSTEM;
    static final String SUB_DEPLOYMENT = "subdeployment";

    static ModelNode getSubsystemRoot(final String subsystemName, final DeploymentUnit unit) {
        final Resource root = unit.getAttachment(DEPLOYMENT_RESOURCE);
        synchronized (root) {
            return getOrCreate(root, PathElement.pathElement(SUBSYSTEM, subsystemName)).getModel();
        }
    }

    static ModelNode createDeploymentSubModel(final String subsystemName, final PathElement address, final DeploymentUnit unit) {
        final Resource root = unit.getAttachment(DEPLOYMENT_RESOURCE);
        synchronized (root) {
            final ImmutableManagementResourceRegistration registration = unit.getAttachment(REGISTRATION_ATTACHMENT);
            final Resource subsystem = getOrCreate(root, PathElement.pathElement(SUBSYSTEM, subsystemName));
            final ImmutableManagementResourceRegistration subModel = registration.getSubModel(getExtensionAddress(subsystemName, address));
            if(subModel == null) {
                throw new IllegalStateException(address.toString());
            }
            return getOrCreate(subsystem, address).getModel();
        }
    }

    static Resource createSubDeployment(final String deploymentName, DeploymentUnit parent) {
        final Resource root = parent.getAttachment(DEPLOYMENT_RESOURCE);
        return getOrCreate(root, PathElement.pathElement(SUB_DEPLOYMENT, deploymentName));
    }

    static Resource getOrCreate(final Resource parent, final PathElement element) {
        synchronized(parent) {
            if(parent.hasChild(element)) {
                return parent.requireChild(element);
            } else {
                final Resource resource = Resource.Factory.create();
                parent.registerChild(element, resource);
                return resource;
            }
        }
    }

    static void cleanup(final Resource resource) {
        synchronized (resource) {
            for(final Resource.ResourceEntry entry : resource.getChildren(SUBSYSTEM)) {
                resource.removeChild(entry.getPathElement());
            }
            for(final Resource.ResourceEntry entry : resource.getChildren(SUB_DEPLOYMENT)) {
                resource.removeChild(entry.getPathElement());
            }
        }
    }

    static PathAddress getExtensionAddress(final String subsystemName, final PathElement element) {
        return PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystemName), element);
    }

}
