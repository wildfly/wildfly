
/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jdr;

import static org.jboss.as.jdr.JdrReportSubsystemDefinition.EXECUTOR_CAPABILITY;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
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
                registerServiceCapabilities(capabilityRegistry, Collections.singletonMap(EXECUTOR_CAPABILITY, ExecutorService.class));
            }
        };
    }
}
