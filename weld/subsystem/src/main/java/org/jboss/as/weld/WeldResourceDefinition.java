/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource definition for Weld subsystem
 *
 * @author Jozef Hartinger
 *
 */
class WeldResourceDefinition extends PersistentResourceDefinition {
    static final RuntimeCapability<WeldCapability> WELD_CAPABILITY = RuntimeCapability.Builder
            .of(WELD_CAPABILITY_NAME, WeldCapabilityImpl.INSTANCE)
            .build();

    static final String REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE_NAME = "require-bean-descriptor";
    static final String LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE_NAME = "legacy-empty-beans-xml-treatment";
    static final String NON_PORTABLE_MODE_ATTRIBUTE_NAME = "non-portable-mode";
    static final String DEVELOPMENT_MODE_ATTRIBUTE_NAME = "development-mode";
    static final String THREAD_POOL_SIZE = "thread-pool-size";

    static final SimpleAttributeDefinition REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE_NAME, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE_NAME, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .setRestartAllServices()
                    .build();

    static final SimpleAttributeDefinition NON_PORTABLE_MODE_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(NON_PORTABLE_MODE_ATTRIBUTE_NAME, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition DEVELOPMENT_MODE_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(DEVELOPMENT_MODE_ATTRIBUTE_NAME, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .setRestartAllServices()
            .setDeprecated(ModelVersion.create(5, 0))
            .build();

    static final SimpleAttributeDefinition THREAD_POOL_SIZE_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(THREAD_POOL_SIZE, ModelType.INT, true)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1))
            .setRestartAllServices()
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE, LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE, NON_PORTABLE_MODE_ATTRIBUTE, DEVELOPMENT_MODE_ATTRIBUTE, THREAD_POOL_SIZE_ATTRIBUTE };

    WeldResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(WeldExtension.PATH_SUBSYSTEM, WeldExtension.getResourceDescriptionResolver())
                .setAddHandler(new WeldSubsystemAdd(ATTRIBUTES))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(WELD_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.passive("org.jboss.as.weld.ejb"),
                    RuntimePackageDependency.passive("org.jboss.as.weld.jpa"),
                    RuntimePackageDependency.passive("org.jboss.as.weld.beanvalidation"),
                    RuntimePackageDependency.passive("org.jboss.as.weld.webservices"),
                    RuntimePackageDependency.passive("org.jboss.as.weld.transactions"),
                    RuntimePackageDependency.required("jakarta.inject.api"),
                    RuntimePackageDependency.required("jakarta.persistence.api"),
                    RuntimePackageDependency.required("org.hibernate.validator.cdi"));
    }
}
