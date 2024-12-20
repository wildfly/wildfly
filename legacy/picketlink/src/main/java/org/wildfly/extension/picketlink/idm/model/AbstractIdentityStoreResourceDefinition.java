/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.ModelValidationStepHandler;

public abstract class AbstractIdentityStoreResourceDefinition extends AbstractIDMResourceDefinition {

    public static final SimpleAttributeDefinition SUPPORT_ATTRIBUTE = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_STORE_SUPPORT_ATTRIBUTE.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.TRUE)
        .setAllowExpression(true)
        .build();

    public static final SimpleAttributeDefinition SUPPORT_CREDENTIAL = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_STORE_SUPPORT_CREDENTIAL.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.TRUE)
        .setAllowExpression(true)
        .build();

    protected AbstractIdentityStoreResourceDefinition(ModelElement modelElement, ModelValidationStepHandler[] modelValidators, SimpleAttributeDefinition... attributes) {
        super(modelElement, modelValidators, address -> address.getParent().getParent(), attributes);
    }
}
