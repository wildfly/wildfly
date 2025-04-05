/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
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
public class JGroupsTransformersTestCase extends AbstractSubsystemTest {
    private static final Map<ModelTestControllerVersion, JGroupsSubsystemModel> VERSIONS = new EnumMap<>(ModelTestControllerVersion.class);
    static {
        VERSIONS.put(ModelTestControllerVersion.EAP_7_4_0, JGroupsSubsystemModel.VERSION_8_0_0);
        VERSIONS.put(ModelTestControllerVersion.EAP_8_0_0, JGroupsSubsystemModel.VERSION_10_0_0);
    }

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return VERSIONS.keySet();
    }

    private final ModelTestControllerVersion controllerVersion;
    private final ModelVersion subsystemVersion;

    public JGroupsTransformersTestCase(ModelTestControllerVersion version) {
        super(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new JGroupsExtension());
        this.controllerVersion = version;
        this.subsystemVersion = VERSIONS.get(version).getVersion();
    }

    private String[] getDependencies() {
        switch (this.controllerVersion) {
            case EAP_7_4_0:
                return new String[] {
                        this.formatArtifact("wildfly-clustering-jgroups-extension"),
                        this.formatArtifact("wildfly-clustering-api"),
                        this.formatArtifact("wildfly-clustering-common"),
                        this.formatArtifact("wildfly-clustering-jgroups-spi"),
                        this.formatArtifact("wildfly-clustering-server"),
                        this.formatArtifact("wildfly-clustering-service"),
                        this.formatArtifact("wildfly-clustering-spi"),
                };
            case EAP_8_0_0:
                return new String[] {
                        this.formatArtifact("wildfly-clustering-jgroups-extension"),
                        this.formatArtifact("wildfly-clustering-common"),
                        this.formatArtifact("wildfly-clustering-jgroups-spi"),
                        this.formatArtifact("wildfly-clustering-server-service"),
                        this.formatArtifact("wildfly-clustering-server-spi"),
                        this.formatArtifact("wildfly-clustering-service"),
                };
            default:
                throw new IllegalArgumentException();
        }
    }

    private String formatArtifact(String artifactId) {
        return String.format("%s:%s:%s", this.controllerVersion.getMavenGroupId(), artifactId, this.controllerVersion.getMavenGavVersion());
    }

    private static AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(SocketBinding.SERVICE_DESCRIPTOR, List.of("jgroups-tcp", "jgroups-udp", "jgroups-udp-fd", "some-binding", "client-binding", "jgroups-diagnostics", "jgroups-mping", "jgroups-tcp-fd", "jgroups-client-fd", "jgroups-state-xfr"))
                .require(CommonServiceDescriptor.KEY_STORE, "my-key-store")
                .require(CommonServiceDescriptor.CREDENTIAL_STORE, "my-credential-store")
                ;
    }

    private KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(createAdditionalInitialization());
    }

    private KernelServices build(KernelServicesBuilder builder) throws Exception {
        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), this.controllerVersion, this.subsystemVersion)
                .addMavenResourceURL(this.getDependencies())
                .addSingleChildFirstClass(AdditionalInitialization.class)
                // workaround IllegalArgumentException: key 1100 (org.jboss.as.clustering.jgroups.auth.BinaryAuthToken) is already in magic map; make sure that all keys are unique
                .addSingleChildFirstClass(ClassConfigurator.class)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();

        Assert.assertTrue(services.getBootErrorDescription(), services.isSuccessfulBoot());

        KernelServices legacyServices = services.getLegacyServices(this.subsystemVersion);

        Assert.assertTrue(legacyServices.getBootErrorDescription(), legacyServices.isSuccessfulBoot());

        return services;
    }

    @Test
    public void testTransformations() throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder().setSubsystemXmlResource(String.format("jgroups-transform-%s.xml", this.subsystemVersion.toString()));
        KernelServices services = this.build(builder);

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, this.subsystemVersion, null, false);
    }

    @Test
    public void testRejections() throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder();
        KernelServices services = this.build(builder);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement());

        if (JGroupsSubsystemModel.VERSION_8_0_0.requiresTransformation(this.subsystemVersion)) {
            config.addFailedAttribute(subsystemAddress.append(JGroupsResourceRegistration.STACK.pathElement("credentialReference1")).append(StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement("SYM_ENCRYPT")), FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }

        List<ModelNode> operations = builder.parseXmlResource("jgroups-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, this.subsystemVersion, operations, config);
    }
}
