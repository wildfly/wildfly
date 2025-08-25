/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
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
        return EnumSet.of(
                ModelTestControllerVersion.EAP_7_4_0,
                ModelTestControllerVersion.EAP_8_0_0,
                ModelTestControllerVersion.EAP_8_1_0
        );
    }

    private final ModelTestControllerVersion controllerVersion;

    public ModClusterTransformersTestCase(ModelTestControllerVersion controllerVersion) {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension());
        this.controllerVersion = controllerVersion;
    }

    private static ModClusterSubsystemModel getModelVersion(ModelTestControllerVersion controllerVersion) {
        return switch (controllerVersion) {
            case EAP_7_4_0 -> ModClusterSubsystemModel.VERSION_7_0_0;
            case EAP_8_0_0, EAP_8_1_0 -> ModClusterSubsystemModel.VERSION_8_0_0;
            default -> throw new IllegalArgumentException();
        };
    }

    private String[] getDependencies(ModelTestControllerVersion version) {
        return switch (version) {
            case EAP_7_4_0 -> new String[] {
                    "org.jboss.mod_cluster:mod_cluster-core:1.4.3.Final-redhat-00002",
                    this.controllerVersion.createGAV("wildfly-clustering-common"),
                    this.controllerVersion.createGAV("wildfly-mod_cluster-extension"),
            };
            case EAP_8_0_0 -> new String[] {
                    "org.jboss.mod_cluster:mod_cluster-core:2.0.1.Final-redhat-00001",
                    this.controllerVersion.createGAV("wildfly-clustering-common"),
                    this.controllerVersion.createGAV("wildfly-mod_cluster-extension"),
            };
            case EAP_8_1_0 -> new String[] {
                    // TODO Replace with -redhat version once EAP 8.1 is released
                    "org.jboss.mod_cluster:mod_cluster-core:2.1.0.Final",
                    this.controllerVersion.createGAV("wildfly-clustering-common"),
                    this.controllerVersion.createGAV("wildfly-mod_cluster-extension"),
            };
            default -> throw new IllegalArgumentException();
        };
    }

    @Test
    public void testTransformations() throws Exception {
        this.testTransformations(controllerVersion);
    }

    private void testTransformations(ModelTestControllerVersion controllerVersion) throws Exception {
        ModClusterSubsystemModel model = getModelVersion(controllerVersion);
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

            checkSubsystemModelTransformation(mainServices, modelVersion, createModelFixer(modelVersion), false);
        }
    }

    private static ModelFixer createModelFixer(ModelVersion version) {
        return model -> {
            if (ModClusterSubsystemModel.VERSION_8_0_0.requiresTransformation(version)) {
                Set.of("default", "with-floating-decay-load-provider").forEach(
                        proxy -> model.get(ProxyConfigurationResourceDefinition.pathElement(proxy).getKeyValuePair()).get("connector").set(new ModelNode())
                );
            }
            return model;
        };
    }

    @Test
    public void testRejections() throws Exception {
        this.testRejections(controllerVersion);
    }

    private void testRejections(ModelTestControllerVersion controllerVersion) throws Exception {
        String[] dependencies = getDependencies(controllerVersion);
        String subsystemXml = readResource("subsystem-reject.xml");
        ModClusterSubsystemModel model = getModelVersion(controllerVersion);
        ModelVersion modelVersion = model.getVersion();

        KernelServicesBuilder builder = createKernelServicesBuilder(new ModClusterAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(new ModClusterAdditionalInitialization(), controllerVersion, modelVersion)
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
