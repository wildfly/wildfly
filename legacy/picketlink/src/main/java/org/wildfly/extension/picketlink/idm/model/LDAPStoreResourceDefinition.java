/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.ModelValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.NotEmptyResourceValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.RequiredChildValidationStepHandler;
import org.wildfly.extension.picketlink.idm.IDMExtension;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class LDAPStoreResourceDefinition extends AbstractIdentityStoreResourceDefinition {

    public static final SensitiveTargetAccessConstraintDefinition BASE_DN_SUFFIX_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification(IDMExtension.SUBSYSTEM_NAME, "base-dn-suffix", false, true, true));

    public static final SimpleAttributeDefinition URL = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_URL.getName(), ModelType.STRING, false)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition BIND_DN = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_BIND_DN.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition BIND_CREDENTIAL = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_BIND_CREDENTIAL.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition BASE_DN_SUFFIX = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_BASE_DN_SUFFIX.getName(), ModelType.STRING, false)
        .setAccessConstraints(BASE_DN_SUFFIX_CONSTRAINT)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition ACTIVE_DIRECTORY = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_ACTIVE_DIRECTORY.getName(), ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();
    public static final SimpleAttributeDefinition UNIQUE_ID_ATTRIBUTE_NAME = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_UNIQUE_ID_ATTRIBUTE_NAME
            .getName(), ModelType.STRING, true)
            .setAllowExpression(true)
            .build();

    public static final LDAPStoreResourceDefinition INSTANCE = new LDAPStoreResourceDefinition(URL, BIND_DN, BIND_CREDENTIAL, BASE_DN_SUFFIX, SUPPORT_ATTRIBUTE, SUPPORT_CREDENTIAL, ACTIVE_DIRECTORY,
            UNIQUE_ID_ATTRIBUTE_NAME);

    private LDAPStoreResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.LDAP_STORE, getModelValidators(), attributes);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(LDAPStoreMappingResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(SupportedTypesResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(CredentialHandlerResourceDefinition.INSTANCE, resourceRegistration);
    }

    private static ModelValidationStepHandler[] getModelValidators() {
        return new ModelValidationStepHandler[] {
            NotEmptyResourceValidationStepHandler.INSTANCE,
            new RequiredChildValidationStepHandler(ModelElement.SUPPORTED_TYPES)
        };
    }

}
