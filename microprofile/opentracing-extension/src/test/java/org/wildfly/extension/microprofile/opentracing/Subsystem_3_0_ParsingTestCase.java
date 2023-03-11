/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.opentracing;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.MICROPROFILE_CONFIG_CAPABILITY_NAME;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.AdditionalInitialization.ManagementAdditionalInitialization;
import org.jboss.as.weld.WeldCapability;

public class Subsystem_3_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_3_0_ParsingTestCase() {
        super(SubsystemExtension.SUBSYSTEM_NAME, new SubsystemExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_3_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-microprofile-opentracing_3_0.xsd";
    }

    @Override
    protected Properties getResolvedProperties() {
        return System.getProperties();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return OpentracingAdditionalInitialization.INSTANCE;
    }

    private static class OpentracingAdditionalInitialization extends ManagementAdditionalInitialization {

        public static final AdditionalInitialization INSTANCE = new OpentracingAdditionalInitialization();
        private static final long serialVersionUID = 1L;

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            Map<String, Class> capabilities = new HashMap<>();
            capabilities.put(WELD_CAPABILITY_NAME, WeldCapability.class);
            capabilities.put(MICROPROFILE_CONFIG_CAPABILITY_NAME, Void.class);
            registerServiceCapabilities(capabilityRegistry, capabilities);
        }
    }
}
