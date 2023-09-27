/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * {@link org.jboss.as.controller.RestartParentWriteAttributeHandler} that leverages a {@link ResourceServiceBuilderFactory} for service recreation.
 * @author Paul Ferraro
 */
public class RestartParentResourceWriteAttributeHandler extends RestartParentWriteAttributeHandler implements ManagementRegistrar<ManagementResourceRegistration> {

    private final WriteAttributeStepHandlerDescriptor descriptor;
    private final ResourceServiceConfiguratorFactory parentFactory;

    public RestartParentResourceWriteAttributeHandler(ResourceServiceConfiguratorFactory parentFactory, WriteAttributeStepHandlerDescriptor descriptor) {
        super(null, descriptor.getAttributes());
        this.descriptor = descriptor;
        this.parentFactory = parentFactory;
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        this.parentFactory.createServiceConfigurator(parentAddress).configure(context, parentModel).build(context.getServiceTarget()).install();
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return this.parentFactory.createServiceConfigurator(parentAddress).getServiceName();
    }

    @Override
    protected PathAddress getParentAddress(PathAddress address) {
        return address.getParent();
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (AttributeDefinition attribute : this.descriptor.getAttributes()) {
            registration.registerReadWriteAttribute(attribute, null, this);
        }
    }
}
