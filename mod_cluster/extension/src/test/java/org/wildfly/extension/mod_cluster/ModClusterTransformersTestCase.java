/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Radoslav Husar
 */
@RunWith(Parameterized.class)
public class ModClusterTransformersTestCase extends AbstractSubsystemTest {

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return EnumSet.of(ModelTestControllerVersion.EAP_7_4_0);
    }

    ModelTestControllerVersion version;

    public ModClusterTransformersTestCase(ModelTestControllerVersion version) {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension());
        this.version = version;
    }

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    private static ModClusterModel getModelVersion(ModelTestControllerVersion controllerVersion) {
        switch (controllerVersion) {
            case EAP_7_4_0:
                return ModClusterModel.VERSION_7_0_0;
        }
        throw new IllegalArgumentException();
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        switch (version) {
            case EAP_7_4_0:
                return new String[] {
                        formatArtifact("org.jboss.eap:wildfly-mod_cluster-extension:%s", version),
                        "org.jboss.mod_cluster:mod_cluster-core:1.4.3.Final-redhat-00002",
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                };
        }
        throw new IllegalArgumentException();
    }

    @Test
    public void testTransformations() throws Exception {
        this.testTransformations(version);
    }

    private void testTransformations(ModelTestControllerVersion controllerVersion) throws Exception {
        ModClusterModel model = getModelVersion(controllerVersion);
        ModelVersion modelVersion = model.getVersion();
        String[] dependencies = getDependencies(controllerVersion);


        Set<String> resources = new HashSet<>();
        resources.add(String.format("subsystem-transform-%d_%d_%d.xml", modelVersion.getMajor(), modelVersion.getMinor(), modelVersion.getMicro()));


        for (String resource : resources) {
            String subsystemXml = readResource(resource);

            KernelServicesBuilder builder = createKernelServicesBuilder(new ModClusterAdditionalInitialization())
                    .setSubsystemXml(subsystemXml);
            builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                    .addMavenResourceURL(dependencies)
                    .skipReverseControllerCheck()
                    .dontPersistXml();

            KernelServices mainServices = builder.build();
            KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

            Assert.assertNotNull(legacyServices);
            Assert.assertTrue(mainServices.isSuccessfulBoot());
            Assert.assertTrue(legacyServices.isSuccessfulBoot());

            checkSubsystemModelTransformation(mainServices, modelVersion, null, false);
        }
    }

    @Test
    public void testRejections() throws Exception {
        this.testRejections(version);
    }

    private void testRejections(ModelTestControllerVersion controllerVersion) throws Exception {
        String[] dependencies = getDependencies(controllerVersion);
        String subsystemXml = readResource("subsystem-reject.xml");
        ModClusterModel model = getModelVersion(controllerVersion);
        ModelVersion modelVersion = model.getVersion();

        KernelServicesBuilder builder = createKernelServicesBuilder(new ModClusterAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(model.getVersion().getMajor() >= 4 ? new ModClusterAdditionalInitialization() : null, controllerVersion, modelVersion)
                .addSingleChildFirstClass(ModClusterAdditionalInitialization.class)
                .addMavenResourceURL(dependencies)
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(subsystemXml), createFailedOperationConfig(modelVersion));
    }

    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {
        return new FailedOperationTransformationConfig();
    }

}
