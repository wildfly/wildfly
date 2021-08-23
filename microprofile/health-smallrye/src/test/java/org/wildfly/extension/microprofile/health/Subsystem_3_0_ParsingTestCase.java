/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.health;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.microprofile.health.MicroProfileHealthExtension.VERSION_2_0_0;
import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.HEALTH_HTTP_CONTEXT_CAPABILITY;
import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.HEALTH_SERVER_PROBE_CAPABILITY;

/**
 * @author <a href="http://xstefank.io/">Martin Stefanko</a> (c) 2021 Red Hat inc.
 */
public class Subsystem_3_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_3_0_ParsingTestCase() {
        super(MicroProfileHealthExtension.SUBSYSTEM_NAME, new MicroProfileHealthExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_3_0.xml");
    }

    @Override
    protected String[] getSubsystemTemplatePaths() {
        return new String[] {
                "/subsystem-templates/microprofile-health-smallrye.xml"
        };
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-microprofile-health-smallrye_3_0.xsd";
    }

    protected Properties getResolvedProperties() {
        return System.getProperties();
    }

    @Test
    public void testTransformersWildfly24() throws Exception {
        testTransformers(ModelTestControllerVersion.MASTER, VERSION_2_0_0);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion healthExtensionVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_3_0.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, healthExtensionVersion)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(healthExtensionVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, healthExtensionVersion);
    }

    @Test
    public void testRejectingTransformersEAP_7_3_0() throws Exception {
        testRejectingTransformers(ModelTestControllerVersion.EAP_7_3_0, VERSION_2_0_0);
    }

    private static String getMicroProfileSmallryeHeatlhGAV(ModelTestControllerVersion version) {
        if (version.isEap()) {
            return "org.jboss.eap:wildfly-microprofile-health-smallrye:" + version.getMavenGavVersion();
        }
        return "org.wildfly:wildfly-microprofile-health-smallrye:" + version.getMavenGavVersion();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                WELD_CAPABILITY_NAME,
                "org.wildfly.management.executor",
                "org.wildfly.management.http.extensible",
                HEALTH_HTTP_CONTEXT_CAPABILITY,
                HEALTH_SERVER_PROBE_CAPABILITY);
    }

    private void testRejectingTransformers(ModelTestControllerVersion controllerVersion, ModelVersion healthVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, healthVersion)
                .addMavenResourceURL(getMicroProfileSmallryeHeatlhGAV(controllerVersion))
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(healthVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("subsystem_3_0_reject_transform.xml");
        PathAddress subsystemAddress = PathAddress.pathAddress(MicroProfileHealthExtension.SUBSYSTEM_PATH);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        if (healthVersion.equals(VERSION_2_0_0)) {
            config.addFailedAttribute(subsystemAddress,
                new FailedOperationTransformationConfig.NewAttributesConfig(
                    MicroProfileHealthSubsystemDefinition.EMPTY_STARTUP_CHECKS_STATUS));
        }
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, healthVersion, ops, config);
    }
}
