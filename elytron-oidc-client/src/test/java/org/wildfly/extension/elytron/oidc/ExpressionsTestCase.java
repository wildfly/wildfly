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
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.version.Stability;
import org.junit.Assert;
import org.junit.Test;

/**
 * Subsystem parsing test case.
 *
 * <a href="mailto:araskar@redhat.com">Ashpan Raskar</a>
 */
public class ExpressionsTestCase extends AbstractSubsystemSchemaTest<ElytronOidcSubsystemSchema> {

    private KernelServices services = null;

    public ExpressionsTestCase() {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension(), ElytronOidcSubsystemSchema.VERSION_3_0_COMMUNITY, ElytronOidcSubsystemSchema.CURRENT.get(Stability.COMMUNITY));
    }

    @Test
    public void testExpressions() throws Throwable {
        if (services != null) return;
        String subsystemXml = "oidc-expressions.xml";
        services = super.createKernelServicesBuilder(new DefaultInitializer(this.getSubsystemSchema().getStability())).setSubsystemXmlResource(subsystemXml).build();
        if (! services.isSuccessfulBoot()) {
            Assert.fail(services.getBootError().toString());
        }
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) {
        //
    }

    protected static class DefaultInitializer extends AdditionalInitialization {

        private final Stability stability;

        public DefaultInitializer(Stability stability) {
            this.stability = stability;
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            registerCapabilities(capabilityRegistry, ELYTRON_CAPABILITY_NAME);
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

        @Override
        public Stability getStability() {
            return stability;
        }

    }

}