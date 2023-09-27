/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcSubsystemDefinition.ELYTRON_CAPABILITY_NAME;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.junit.Assert;
import org.junit.Test;

/**
 * Subsystem parsing test case.
 *
 * <a href="mailto:araskar@redhat.com">Ashpan Raskar</a>
 */
public class ExpressionsTestCase extends AbstractSubsystemTest {

    private KernelServices services = null;

    public ExpressionsTestCase() {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension());
    }

    @Test
    public void testExpressions() throws Throwable {
        if (services != null) return;
        String subsystemXml = "oidc-expressions.xml";
        services = super.createKernelServicesBuilder(new DefaultInitializer()).setSubsystemXmlResource(subsystemXml).build();
        if (! services.isSuccessfulBoot()) {
            Assert.fail(services.getBootError().toString());
        }
    }

    private static class DefaultInitializer extends AdditionalInitialization {

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            registerCapabilities(capabilityRegistry, ELYTRON_CAPABILITY_NAME);
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

    }

}