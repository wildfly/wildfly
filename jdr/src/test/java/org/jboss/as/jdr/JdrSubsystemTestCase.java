
/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.Executor;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.management.Capabilities;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * Performs basic parsing and configuration testing of the JDR subsystem.
 *
 * @author Mike M. Clark
 */
@RunWith(Parameterized.class)
public class JdrSubsystemTestCase extends AbstractSubsystemBaseTest {
    @Parameters
    public static Iterable<JdrReportSubsystemSchema> parameters() {
        return EnumSet.allOf(JdrReportSubsystemSchema.class);
    }

    private final JdrReportSubsystemSchema schema;

    public JdrSubsystemTestCase(JdrReportSubsystemSchema schema) {
        super(JdrReportExtension.SUBSYSTEM_NAME, new JdrReportExtension());
        this.schema = schema;
    }

    @Test(expected = XMLStreamException.class)
    public void testParseSubsystemWithBadChild() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + this.schema.getNamespace() + "\"><invalid/></subsystem>";
        super.parse(subsystemXml);
    }

    @Test(expected = XMLStreamException.class)
    public void testParseSubsystemWithBadAttribute() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + this.schema.getNamespace() + "\" attr=\"wrong\"/>";
        super.parse(subsystemXml);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"" + this.schema.getNamespace() + "\"/>";
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return String.format(Locale.ROOT, "schema/jboss-as-jdr_%d_%d.xsd", this.schema.getVersion().major(), this.schema.getVersion().minor());
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.NORMAL;
            }

            @Override
            protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
                super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
                registerServiceCapabilities(capabilityRegistry, Collections.singletonMap(Capabilities.MANAGEMENT_EXECUTOR.getName(), Executor.class));
            }
        };
    }
}
