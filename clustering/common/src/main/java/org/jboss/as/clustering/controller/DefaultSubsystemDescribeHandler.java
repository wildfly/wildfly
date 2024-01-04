/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
public class DefaultSubsystemDescribeHandler extends GenericSubsystemDescribeHandler implements ManagementRegistrar<ManagementResourceRegistration> {

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
