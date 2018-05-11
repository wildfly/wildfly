/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.jboss.as.clustering.controller.transform.InitialAttributeValueOperationContextAttachment;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformerOperationAttachment;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * {@link org.jboss.as.controller.RestartParentWriteAttributeHandler} that leverages a {@link ResourceServiceBuilderFactory} for service recreation.
 * @author Paul Ferraro
 */
public class RestartParentResourceWriteAttributeHandler extends RestartParentWriteAttributeHandler implements Registration<ManagementResourceRegistration> {

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

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode oldValue, Resource model) throws OperationFailedException {
        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);

        if (!context.isBooting()) {
            TransformerOperationAttachment attachment = TransformerOperationAttachment.getOrCreate(context);
            InitialAttributeValueOperationContextAttachment valuesAttachment = attachment.getAttachment(InitialAttributeValueOperationContextAttachment.INITIAL_VALUES_ATTACHMENT);
            if (valuesAttachment == null) {
                valuesAttachment = new InitialAttributeValueOperationContextAttachment();
                attachment.attach(InitialAttributeValueOperationContextAttachment.INITIAL_VALUES_ATTACHMENT, valuesAttachment);
            }
            valuesAttachment.putIfAbsentInitialValue(Operations.getPathAddress(operation), attributeName, oldValue);
        }
    }
}
