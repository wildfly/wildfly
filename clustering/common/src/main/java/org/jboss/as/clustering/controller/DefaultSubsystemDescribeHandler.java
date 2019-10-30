/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.Optional;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.common.OrderedChildTypesAttachment;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Custom subsystem describe operation handler that works with override models.
 * Workaround for WFCORE-2286.
 * @author Paul Ferraro
 */
public class DefaultSubsystemDescribeHandler extends GenericSubsystemDescribeHandler implements Registration<ManagementResourceRegistration> {

    @Override
    protected void describe(OrderedChildTypesAttachment orderedChildTypesAttachment, Resource resource, ModelNode addressModel, ModelNode result, ImmutableManagementResourceRegistration registration) {
        if (resource == null || registration.isRemote() || registration.isRuntimeOnly() || resource.isProxy() || resource.isRuntime() || registration.isAlias()) return;
        result.add(createAddOperation(orderedChildTypesAttachment, addressModel, resource, registration.getChildAddresses(PathAddress.EMPTY_ADDRESS)));
        PathAddress address = PathAddress.pathAddress(addressModel);
        for (String type : resource.getChildTypes()) {
            for (Resource.ResourceEntry entry : resource.getChildren(type)) {
                PathElement path = entry.getPathElement();
                ImmutableManagementResourceRegistration childRegistration = Optional.ofNullable(registration.getSubModel(PathAddress.pathAddress(path))).orElse(registration.getSubModel(PathAddress.pathAddress(PathElement.pathElement(path.getKey()))));
                PathAddress childAddress = address.append(path);
                this.describe(orderedChildTypesAttachment, entry, childAddress.toModelNode(), result, childRegistration);
            }
        }
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, this);
    }
}
