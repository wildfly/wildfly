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

import static org.jboss.as.subsystem.test.AdditionalInitialization.registerServiceCapabilities;
import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.MICROPROFILE_CONFIG_CAPABILITY_NAME;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.AdditionalInitialization.ManagementAdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.weld.WeldCapability;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

public class Subsystem_3_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_3_0_ParsingTestCase() {
        super(SubsystemExtension.SUBSYSTEM_NAME, new SubsystemExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_3_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-microprofile-opentracing_3_0.xsd";
    }

    @Override
    protected Properties getResolvedProperties() {
        return System.getProperties();
    }

    @Test
    public void testTransformersWildFly20() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_3_0, SubsystemExtension.VERSION_1_0_0);
    }

    @Test
    public void testRejectingTransformersWildFly20() throws Exception {
        testRejectingTransformers(ModelTestControllerVersion.EAP_7_3_0, SubsystemExtension.VERSION_1_0_0);
    }

    @Test
    public void testTransformersWildFly19() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_2_0, SubsystemExtension.VERSION_1_0_0);
    }

    @Test
    public void testRejectingTransformersWildFly19() throws Exception {
        testRejectingTransformers(ModelTestControllerVersion.EAP_7_2_0, SubsystemExtension.VERSION_1_0_0);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion opentracingVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_3_0_transform.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, opentracingVersion)
                .addMavenResourceURL(String.format("%s:wildfly-microprofile-opentracing-extension:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addMavenResourceURL(String.format("%s:wildfly-microprofile-opentracing-smallrye:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addMavenResourceURL(String.format("%s:wildfly-weld:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addMavenResourceURL(String.format("%s:wildfly-weld-common:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addSingleChildFirstClass(OpentracingAdditionalInitialization.class)
                .skipReverseControllerCheck()
                .dontPersistXml();
        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(opentracingVersion).isSuccessfulBoot());
        checkSubsystemModelTransformation(mainServices, opentracingVersion);
    }

    private void testRejectingTransformers(ModelTestControllerVersion controllerVersion, ModelVersion opentracingVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, opentracingVersion)
                .addMavenResourceURL(String.format("%s:wildfly-microprofile-opentracing-extension:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addMavenResourceURL(String.format("%s:wildfly-microprofile-opentracing-smallrye:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addMavenResourceURL(String.format("%s:wildfly-weld:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addMavenResourceURL(String.format("%s:wildfly-weld-common:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addSingleChildFirstClass(OpentracingAdditionalInitialization.class)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(opentracingVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("subsystem_3_0_reject_transform.xml");
        PathAddress subsystemAddress = PathAddress.pathAddress(SubsystemExtension.SUBSYSTEM_PATH);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        config.addFailedAttribute(subsystemAddress,
                new FailedOperationTransformationConfig.NewAttributesConfig(
                        SubsystemDefinition.DEFAULT_TRACER))
                .addFailedAttribute(subsystemAddress.append(JaegerTracerConfigurationDefinition.TRACER_CONFIGURATION_PATH),
                        FailedOperationTransformationConfig.REJECTED_RESOURCE);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, opentracingVersion, ops, config);
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
