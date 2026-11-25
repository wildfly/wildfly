/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.ModelValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.NotEmptyResourceValidationStepHandler;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class IdentityConfigurationResourceDefinition extends AbstractIDMResourceDefinition {

    public static final IdentityConfigurationResourceDefinition INSTANCE = new IdentityConfigurationResourceDefinition();

    private IdentityConfigurationResourceDefinition() {
        super(ModelElement.IDENTITY_CONFIGURATION, getModelValidators(), PathAddress::getParent);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(JPAStoreResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(FileStoreResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(LDAPStoreResourceDefinition.INSTANCE, resourceRegistration);
    }

    private static ModelValidationStepHandler[] getModelValidators() {
        return new ModelValidationStepHandler[] {
                NotEmptyResourceValidationStepHandler.INSTANCE
        };
    }
}
