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

package org.wildfly.extension.picketlink.federation.model.idp;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.AlternativeAttributeValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.ElementMaxOccurrenceValidationStepHandler;
import org.wildfly.extension.picketlink.federation.service.IdentityProviderService;

/**
 * @author Pedro Silva
 */
public class IdentityProviderConfigAddStepHandler extends RestartParentResourceAddHandler {

    private final AttributeDefinition[] attributes;
    private final List<AttributeDefinition> alternativeAttributes = new ArrayList<AttributeDefinition>();

    IdentityProviderConfigAddStepHandler(final AttributeDefinition... attributes) {
        super(ModelElement.IDENTITY_PROVIDER.getName());
        this.attributes = attributes != null ? attributes : new AttributeDefinition[0];

        for (AttributeDefinition attribute : this.attributes) {
            if (attribute.getAlternatives() != null && attribute.getAlternatives().length > 0) {
                this.alternativeAttributes.add(attribute);
            }
        }
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(
            new ElementMaxOccurrenceValidationStepHandler(ModelElement.IDENTITY_PROVIDER_ATTRIBUTE_MANAGER, ModelElement.IDENTITY_PROVIDER,
                1), OperationContext.Stage.MODEL);
        context.addStep(
            new ElementMaxOccurrenceValidationStepHandler(ModelElement.IDENTITY_PROVIDER_ROLE_GENERATOR, ModelElement.IDENTITY_PROVIDER,
                1), OperationContext.Stage.MODEL);
        if (!this.alternativeAttributes.isEmpty()) {
            context.addStep(new AlternativeAttributeValidationStepHandler(
                this.alternativeAttributes.toArray(new AttributeDefinition[this.alternativeAttributes.size()]))
                , OperationContext.Stage.MODEL);
        }
        super.execute(context, operation);
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        IdentityProviderAddHandler.launchServices(context, parentModel, parentAddress, true);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return IdentityProviderService.createServiceName(parentAddress.getLastElement().getValue());
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : this.attributes) {
            attr.validateAndSet(operation, model);
        }
    }
}
