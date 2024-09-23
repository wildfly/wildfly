/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.jakarta.data;

import java.util.EnumSet;

import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Standard subsystem tests for the jakarta-data subsystem.
 */
@RunWith(value = Parameterized.class)
public class JakartaDataSubsystemTestCase extends AbstractSubsystemSchemaTest<JakartaDataExtension.JakartaDataSubsystemSchema> {
    @Parameterized.Parameters
    public static Iterable<JakartaDataExtension.JakartaDataSubsystemSchema> parameters() {
        return EnumSet.allOf(JakartaDataExtension.JakartaDataSubsystemSchema.class);
    }

    public JakartaDataSubsystemTestCase(JakartaDataExtension.JakartaDataSubsystemSchema schema) {
        super(JakartaDataExtension.SUBSYSTEM_NAME, new JakartaDataExtension(), schema, JakartaDataExtension.JakartaDataSubsystemSchema.CURRENT);
    }

    @Override
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInit(getSubsystemSchema());
    }

    private static class AdditionalInit extends AdditionalInitialization.ManagementAdditionalInitialization {

        private AdditionalInit(JakartaDataExtension.JakartaDataSubsystemSchema schema) {
            super(schema);
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            registerCapabilities(capabilityRegistry, "org.wildfly.jpa");
        }
    }
}
