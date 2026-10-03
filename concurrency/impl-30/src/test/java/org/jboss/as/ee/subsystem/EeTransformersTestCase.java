/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class EeTransformersTestCase extends AbstractSubsystemTest {
    private static final Map<ModelTestControllerVersion, ModelVersion> VERSIONS = new EnumMap<>(ModelTestControllerVersion.class);

    static {
        VERSIONS.put(ModelTestControllerVersion.WILDFLY_31_0_0, EESubsystemModel.Version.v6_0_0);
        // later versions don't work because ServiceLoader finds the ConcurrencyImplementation from the parent
        // and that doesn't work in the legacy controller. ChildFirstClassloader has no mechanism to prevent that.
        // So, for version 6.0.0 we only test WILDFLY_31_0_0
        //VERSIONS.put(ModelTestControllerVersion.WILDFLY_41_0_0, EESubsystemModel.Version.v6_0_0);
    }

    @Parameterized.Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return VERSIONS.keySet();
    }

    private final ModelTestControllerVersion controllerVersion;
    private final ModelVersion subsystemVersion;

    public EeTransformersTestCase(ModelTestControllerVersion version) {
        super(EeExtension.SUBSYSTEM_NAME, new EeExtension());
        this.controllerVersion = version;
        this.subsystemVersion = VERSIONS.get(version);
    }


    @Test
    public void testTransformers() throws Exception {

        KernelServicesBuilder builder = createKernelServicesBuilder()
                .setSubsystemXmlResource(String.format("ee-transform-%s.xml", this.subsystemVersion.toString()));

        KernelServices mainServices = this.build(builder);

        checkSubsystemModelTransformation(mainServices, this.subsystemVersion, null, true);
    }

    @Test
    public void testRejections() throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder();
        KernelServices services = this.build(builder);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        PathAddress subsystemAddress = PathAddress.pathAddress(EeExtension.PATH_SUBSYSTEM);

        if (EESubsystemModel.Version.v7_0_0.compareTo(this.subsystemVersion) < 0) {
            // for the EE 10 impl we do not expect any rejects because neither the DC nor the legacy host
            // provides virtual threads
        }

        List<ModelNode> operations = builder.parseXmlResource("ee-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, this.subsystemVersion, operations, config);
    }

    private KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(createAdditionalInitialization());
    }

    private KernelServices build(KernelServicesBuilder builder) throws Exception {

        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), this.controllerVersion, this.subsystemVersion)
                .addMavenResourceURL(getDependencies())
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(this.subsystemVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        return mainServices;
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities("org.wildfly.management.path-manager");
    }



    private String[] getDependencies() {
        return switch (this.controllerVersion) {
            case WILDFLY_31_0_0 -> new String[] {
                    this.controllerVersion.createGAV("wildfly-ee"),
                    "org.glassfish:jakarta.enterprise.concurrent:3.0.0"
            };
//            case WILDFLY_41_0_0 -> new String[] {
//                    this.controllerVersion.createGAV("wildfly-ee"),
//                    this.controllerVersion.createGAV("wildfly-concurrency-spi"),
//                    this.controllerVersion.createGAV("wildfly-concurrency-impl-31"),
//                    "org.glassfish.concurro:concurro:3.1.0"
//            };
            default -> throw new IllegalArgumentException();
        };
    }

}