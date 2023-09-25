/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.keycloak.subsystem.adapter.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelType;

/**
 * Defines attributes and operations for a credential.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class CredentialDefinition extends ModelOnlyResourceDefinition {

    public static final String TAG_NAME = "credential";

    protected static final AttributeDefinition VALUE =
            new SimpleAttributeDefinitionBuilder("value", ModelType.STRING, false)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
            .build();

    public CredentialDefinition() {
        super(PathElement.pathElement(TAG_NAME),
                KeycloakExtension.getResourceDescriptionResolver(TAG_NAME),
                new ModelOnlyAddStepHandler(VALUE),
                VALUE);
    }

}
