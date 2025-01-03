/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.idp;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.ElementMaxOccurrenceValidationStepHandler;

/**
 * @author Pedro Silva
 */
public class IdentityProviderConfigAddStepHandler extends ModelOnlyAddStepHandler {

    IdentityProviderConfigAddStepHandler(final AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(
            new ElementMaxOccurrenceValidationStepHandler(ModelElement.IDENTITY_PROVIDER_ATTRIBUTE_MANAGER, ModelElement.IDENTITY_PROVIDER,
                1), OperationContext.Stage.MODEL);
        context.addStep(
            new ElementMaxOccurrenceValidationStepHandler(ModelElement.IDENTITY_PROVIDER_ROLE_GENERATOR, ModelElement.IDENTITY_PROVIDER,
                1), OperationContext.Stage.MODEL);
        super.execute(context, operation);
    }
}
