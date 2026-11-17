/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.version.Stability;

import java.io.IOException;

import static org.wildfly.extension.vertx.VertxSubsystemExtension.SUBSYSTEM_NAME;

/**
 * Standard subsystem test.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class SubsystemTestCase extends AbstractSubsystemBaseTest {

    public SubsystemTestCase() {
        super(SUBSYSTEM_NAME, new VertxSubsystemExtension(), Stability.PREVIEW);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        // test configuration put in standalone.xml
        return readResource("vertx-options-full.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-vertx_1_0_0.xsd";
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization.ManagementAdditionalInitialization(Stability.PREVIEW) {

            @Override
            protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                                                            ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
                super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
                registerCapabilities(capabilityRegistry, "org.wildfly.weld");
            }
        };
    }

}
