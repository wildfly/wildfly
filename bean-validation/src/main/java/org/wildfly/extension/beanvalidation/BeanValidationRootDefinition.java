/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.beanvalidation;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;

/**
 * Defines the Jakarta Bean Validation subsystem root resource.
 *
 * @author Eduardo Martins
 */
class BeanValidationRootDefinition extends PersistentResourceDefinition {

    private static final RuntimeCapability<Void> BEAN_VALIDATION_CAPABILITY =
            RuntimeCapability.Builder.of("org.wildfly.bean-validation").build();

    BeanValidationRootDefinition() {
        super (new Parameters(BeanValidationExtension.SUBSYSTEM_PATH, BeanValidationExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(BeanValidationSubsystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(BEAN_VALIDATION_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.passive("org.hibernate.validator"));
    }
}
