/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.keystore;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class KeyStoreProviderResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition FILE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_FILE.getName(), ModelType.STRING, false)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_RELATIVE_TO.getName(), ModelType.STRING, true)
        .setRequires(ModelElement.COMMON_FILE.getName())
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinitionBuilder(ModelElement.KEY_STORE_PASSWORD.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition SIGN_KEY_ALIAS = new SimpleAttributeDefinitionBuilder(ModelElement.KEY_STORE_SIGN_KEY_ALIAS.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition SIGN_KEY_PASSWORD = new SimpleAttributeDefinitionBuilder(ModelElement.KEY_STORE_SIGN_KEY_PASSWORD.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();

    private static final SimpleAttributeDefinition[] ATTRIBUTE_DEFINITIONS = new SimpleAttributeDefinition[] {FILE, RELATIVE_TO, PASSWORD, SIGN_KEY_ALIAS, SIGN_KEY_PASSWORD};

    public static final KeyStoreProviderResourceDefinition INSTANCE = new KeyStoreProviderResourceDefinition();

    private KeyStoreProviderResourceDefinition() {
        super(ModelElement.KEY_STORE, ModelElement.KEY_STORE.getName(), new ModelOnlyAddStepHandler(ATTRIBUTE_DEFINITIONS), ATTRIBUTE_DEFINITIONS);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(KeyResourceDefinition.INSTANCE, resourceRegistration);
    }
}
