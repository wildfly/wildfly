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
            .setDefaultValue(new ModelNode(false))
            .build();
    public static final SimpleAttributeDefinition UNIQUE_ID_ATTRIBUTE_NAME = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_UNIQUE_ID_ATTRIBUTE_NAME
            .getName(), ModelType.STRING, true)
            .setAllowExpression(true)
            .build();

    public static final LDAPStoreResourceDefinition INSTANCE = new LDAPStoreResourceDefinition(URL, BIND_DN, BIND_CREDENTIAL, BASE_DN_SUFFIX, SUPPORT_ATTRIBUTE, SUPPORT_CREDENTIAL, ACTIVE_DIRECTORY,
            UNIQUE_ID_ATTRIBUTE_NAME);

    private LDAPStoreResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.LDAP_STORE, new IDMConfigAddStepHandler(getModelValidators(), attributes), attributes);
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
