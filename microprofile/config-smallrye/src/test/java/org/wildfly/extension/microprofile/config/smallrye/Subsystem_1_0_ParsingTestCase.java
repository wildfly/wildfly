/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.config.smallrye;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.microprofile.config.smallrye.MicroProfileConfigExtension.CONFIG_SOURCE_PATH;
import static org.wildfly.extension.microprofile.config.smallrye.MicroProfileConfigExtension.SUBSYSTEM_PATH;
import static org.wildfly.extension.microprofile.config.smallrye.MicroProfileConfigExtension.VERSION_1_0_0;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

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

public class Subsystem_1_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_1_0_ParsingTestCase() {
        super(MicroProfileConfigExtension.SUBSYSTEM_NAME, new MicroProfileConfigExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_1_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-microprofile-config-smallrye_1_0.xsd";
    }

    protected Properties getResolvedProperties() {
        return System.getProperties();
    }


    @Test
    public void testRejectingTransformersEAP_7_3_0() throws Exception {
        testRejectingTransformers(ModelTestControllerVersion.EAP_7_3_0, VERSION_1_0_0);
    }

    private static String getMicroProfileConfigSmallryeGAV(ModelTestControllerVersion version) {
        if (version.isEap()) {
            return "org.jboss.eap:wildfly-microprofile-config-smallrye:" + version.getMavenGavVersion();
        }
        return "org.wildfly:wildfly-microprofile-config-smallrye:" + version.getMavenGavVersion();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                WELD_CAPABILITY_NAME);
    }

    private void testRejectingTransformers(ModelTestControllerVersion controllerVersion, ModelVersion microprofileConfigVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, microprofileConfigVersion)
                .addMavenResourceURL(getMicroProfileConfigSmallryeGAV(controllerVersion))
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(microprofileConfigVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("subsystem_1_0_reject_transformers.xml");
        PathAddress subsystemAddress = PathAddress.pathAddress(SUBSYSTEM_PATH);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        if (microprofileConfigVersion.equals(VERSION_1_0_0)) {
            config.addFailedAttribute(subsystemAddress.append(CONFIG_SOURCE_PATH.getKey(), "my-config-source-with-ordinal-expression"),
                    new FailedOperationTransformationConfig.NewAttributesConfig(
                            ConfigSourceDefinition.ORDINAL));
        }
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, microprofileConfigVersion, ops, config);
    }

}
