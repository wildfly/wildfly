/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.AlternativeAttributeValidationStepHandler;
import org.wildfly.extension.picketlink.idm.service.PartitionManagerService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pedro Silva
 */
public class IDMConfigAddStepHandler extends RestartParentResourceAddHandler {

    private final AttributeDefinition[] attributes;
    private final List<AttributeDefinition> alternativeAttributes = new ArrayList<AttributeDefinition>();

    IDMConfigAddStepHandler(final AttributeDefinition... attributes) {
        super(ModelElement.PARTITION_MANAGER.getName());
        this.attributes = attributes != null ? attributes : new AttributeDefinition[0];

        for (AttributeDefinition attribute : this.attributes) {
            if (attribute.getAlternatives() != null && attribute.getAlternatives().length > 0) {
                this.alternativeAttributes.add(attribute);
            }
        }
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (!this.alternativeAttributes.isEmpty()) {
            context.addStep(new AlternativeAttributeValidationStepHandler(
                    this.alternativeAttributes.toArray(new AttributeDefinition[this.alternativeAttributes.size()]))
                , OperationContext.Stage.MODEL);
        }
        super.execute(context, operation);
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel, ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final String federationName = parentAddress.getLastElement().getValue();
        PartitionManagerRemoveHandler.INSTANCE.removeIdentityStoreServices(context, parentModel, federationName);
        PartitionManagerAddHandler.INSTANCE.createPartitionManagerService(context, parentAddress.getLastElement()
            .getValue(), parentModel, verificationHandler, null, false);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return PartitionManagerService.createServiceName(parentAddress.getLastElement().getValue());
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : this.attributes) {
            attr.validateAndSet(operation, model);
        }
    }
}
