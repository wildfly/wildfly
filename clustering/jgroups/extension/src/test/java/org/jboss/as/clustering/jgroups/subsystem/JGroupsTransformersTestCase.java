/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jgroups.conf.ClassConfigurator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test cases for transformers used in the JGroups subsystem.
 *
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
@RunWith(Parameterized.class)
public class JGroupsTransformersTestCase extends OperationTestCaseBase {

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return EnumSet.of(ModelTestControllerVersion.EAP_7_4_0, ModelTestControllerVersion.EAP_8_0_0);
    }

    public JGroupsTransformersTestCase(ModelTestControllerVersion version) {
        super();
        this.version = version;
    }

    ModelTestControllerVersion version;

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    private static JGroupsSubsystemModel getModelVersion(ModelTestControllerVersion controllerVersion) {
        switch (controllerVersion) {
            case EAP_7_4_0:
                return JGroupsSubsystemModel.VERSION_8_0_0;
            case EAP_8_0_0:
                return JGroupsSubsystemModel.VERSION_10_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        switch (version) {
            case EAP_7_4_0:
                return new String[] {
                        formatArtifact("org.jboss.eap:wildfly-clustering-jgroups-extension:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-api:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-jgroups-spi:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-server:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-service:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-spi:%s", version),
                };
            case EAP_8_0_0:
                return new String[] {
                        formatArtifact("org.jboss.eap:wildfly-clustering-jgroups-extension:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-jgroups-spi:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-server-service:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-server-spi:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-service:%s", version),
                };
            default:
                throw new IllegalArgumentException();
        }
    }

    private static org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(CommonUnaryRequirement.SOCKET_BINDING, "jgroups-tcp", "jgroups-udp", "jgroups-udp-fd", "some-binding", "client-binding", "jgroups-diagnostics", "jgroups-mping", "jgroups-tcp-fd", "jgroups-client-fd", "jgroups-state-xfr")
                .require(CommonUnaryRequirement.KEY_STORE, "my-key-store")
                .require(CommonUnaryRequirement.CREDENTIAL_STORE, "my-credential-store")
                ;
    }

    @Test
    public void testTransformations() throws Exception {
        this.testTransformations(version);
    }

    /**
     * Tests transformation of model from current version into specified version.
     */
    private void testTransformations(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String[] dependencies = getDependencies(controller);
        final String subsystemXmlResource = String.format("jgroups-transform-%d_%d_%d.xml", version.getMajor(), version.getMinor(), version.getMicro());

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource(subsystemXmlResource);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controller, version)
                .addMavenResourceURL(dependencies)
                .addSingleChildFirstClass(AdditionalInitialization.class)
                // workaround IllegalArgumentException: key 1100 (org.jboss.as.clustering.jgroups.auth.BinaryAuthToken) is already in magic map; make sure that all keys are unique
                .addSingleChildFirstClass(ClassConfigurator.class)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();

        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(services.getLegacyServices(version).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, null, false);
    }

    @Test
    public void testRejections() throws Exception {
        this.testRejections(version);
    }

    private void testRejections(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String[] dependencies = getDependencies(controller);

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controller, version)
                .addSingleChildFirstClass(AdditionalInitialization.class)
                .addMavenResourceURL(dependencies)
                // workaround IllegalArgumentException: key 1100 (org.jboss.as.clustering.jgroups.auth.BinaryAuthToken) is already in magic map; make sure that all keys are unique
                .addSingleChildFirstClass(ClassConfigurator.class)
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(services.isSuccessfulBoot());
        KernelServices legacyServices = services.getLegacyServices(version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("jgroups-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, version, operations, createFailedOperationTransformationConfig(version));
    }

    private static FailedOperationTransformationConfig createFailedOperationTransformationConfig(ModelVersion version) {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);

        if (JGroupsSubsystemModel.VERSION_8_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(subsystemAddress.append(StackResourceDefinition.pathElement("credentialReference1")).append(ProtocolResourceDefinition.pathElement("SYM_ENCRYPT")),
                    FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }

        return config;
    }
}
