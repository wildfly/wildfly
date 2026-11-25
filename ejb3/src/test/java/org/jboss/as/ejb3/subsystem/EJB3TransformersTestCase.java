/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.EnumMap;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Transform and reject tests for the EJB3 subsystem.
 *
 * @author Radoslav Husar
 */
@RunWith(Parameterized.class)
public class EJB3TransformersTestCase extends AbstractSubsystemTest {

    private static final Map<ModelTestControllerVersion, EJB3Model> VERSIONS = new EnumMap<>(ModelTestControllerVersion.class);

    static {
        VERSIONS.put(ModelTestControllerVersion.EAP_7_4_0, EJB3Model.VERSION_9_0_0);
        VERSIONS.put(ModelTestControllerVersion.EAP_8_0_0, EJB3Model.VERSION_10_0_0);
        VERSIONS.put(ModelTestControllerVersion.EAP_8_1_0, EJB3Model.VERSION_10_0_0);
    }

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return VERSIONS.keySet();
    }

    private final ModelTestControllerVersion controllerVersion;
    private final ModelVersion subsystemVersion;

    public EJB3TransformersTestCase(ModelTestControllerVersion version) {
        super(EJB3Extension.SUBSYSTEM_NAME, new EJB3Extension());

        this.controllerVersion = version;
        this.subsystemVersion = VERSIONS.get(version).getVersion();
    }

    private String[] getDependencies() {
        return switch (this.controllerVersion) {
            case EAP_7_4_0 -> new String[] {
                    "jakarta.persistence:jakarta.persistence-api:2.2.3.redhat-00001",
                    "org.jboss.spec.javax.ejb:jboss-ejb-api_3.2_spec:2.0.0.Final-redhat-00001",
                    "org.jboss.spec.javax.resource:jboss-connector-api_1.7_spec:2.0.0.Final-redhat-00001",
                    "org.jboss.spec.javax.transaction:jboss-transaction-api_1.3_spec:2.0.0.Final-redhat-00005",
                    "org.jboss:jboss-dmr:1.5.1.Final-redhat-00001",
                    this.controllerVersion.createCoreGAV("wildfly-controller"),
                    this.controllerVersion.createCoreGAV("wildfly-threads"),
                    this.controllerVersion.createGAV("wildfly-clustering-api"),
                    this.controllerVersion.createGAV("wildfly-clustering-common"),
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-infinispan"),
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-spi"),
                    this.controllerVersion.createGAV("wildfly-clustering-infinispan-spi"),
                    this.controllerVersion.createGAV("wildfly-clustering-singleton-api"),
                    this.controllerVersion.createGAV("wildfly-clustering-spi"),
                    this.controllerVersion.createGAV("wildfly-ejb3"),
            };
            case EAP_8_0_0 -> new String[] {
                    this.controllerVersion.createCoreGAV("wildfly-threads"),
                    this.controllerVersion.createGAV("wildfly-clustering-service"),
                    this.controllerVersion.createGAV("wildfly-clustering-common"),
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-infinispan"),
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-spi"),
                    this.controllerVersion.createGAV("wildfly-clustering-infinispan-embedded-service"),
                    this.controllerVersion.createGAV("wildfly-ejb3"),
                    this.controllerVersion.createGAV("wildfly-clustering-ee-spi"),
            };
            case EAP_8_1_0 -> new String[] {
                    this.controllerVersion.createCoreGAV("wildfly-controller"),
                    this.controllerVersion.createCoreGAV("wildfly-service"),
                    this.controllerVersion.createCoreGAV("wildfly-subsystem"),
                    this.controllerVersion.createCoreGAV("wildfly-threads"),
                    this.controllerVersion.createGAV("wildfly-clustering-common"),
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-infinispan"),
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-spi"),
                    this.controllerVersion.createGAV("wildfly-clustering-infinispan-embedded-service"),
                    this.controllerVersion.createGAV("wildfly-ejb3"),
            };
            default -> throw new IllegalArgumentException();
        };
    }

    @Test
    public void testTransformations() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem-ejb3-transform.xml");

        builder.createLegacyKernelServicesBuilder(null, this.controllerVersion, this.subsystemVersion)
                .addMavenResourceURL(this.getDependencies())
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(this.subsystemVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(mainServices, this.subsystemVersion, null);
    }

    @Test
    public void testRejections() throws Exception {
        String subsystemXml = readResource("subsystem-ejb3-reject.xml");

        KernelServicesBuilder builder = createKernelServicesBuilder(new EJB3AdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(new EJB3AdditionalInitialization(), this.controllerVersion, this.subsystemVersion)
                .addSingleChildFirstClass(EJB3AdditionalInitialization.class)
                .addMavenResourceURL(this.getDependencies())
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(this.subsystemVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, subsystemVersion, parse(subsystemXml), createFailedOperationConfig(subsystemVersion));
    }

    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(EJB3Extension.SUBSYSTEM_PATH);

        // need to include all changes from current to 9.0.0
        if (EJB3Model.VERSION_10_0_0.requiresTransformation(version)) {
            // Reject /subsystem=ejb3/simple-cache resource
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.SIMPLE_CACHE_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // Reject /subsystem=ejb3/distributable-cache resource
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.DISTRIBUTABLE_CACHE_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // Reject default-persistent-timer-management / default-transient-timer-management attributes
            // and thus the entirety of the timer-service resource
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }

        return config;
    }

}
