/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerSubsystemModel.VERSION_1_1_0;
import static org.wildfly.extension.micrometer.MicrometerSubsystemModel.VERSION_2_0_0;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinitionRegistrar;

/*
* https://github.com/wildfly/wildfly/blob/main/clustering/web/extension/src/test/java/org/wildfly/extension/clustering/web/DistributableWebTransformerTestCase.java#L170-L184
*/

@RunWith(value = Parameterized.class)
public class MicrometerTransformerTestCase extends AbstractSubsystemTest {

    public final AdditionalInitialization additionalInitialization =
            new AdditionalInitialization.ManagementAdditionalInitialization(Stability.PREVIEW);

    private final ModelTestControllerVersion controller;
    private final ModelVersion version;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return List.<Object[]>of(
                new Object[]{ModelTestControllerVersion.EAP_XP_5, VERSION_1_1_0.getVersion()}
        );
    }

    public MicrometerTransformerTestCase(ModelTestControllerVersion controller, ModelVersion version) {
        super(MicrometerConfigurationConstants.NAME, new MicrometerExtension());
        this.controller = controller;
        this.version = version;
    }

    @Test
    public void testTransformation() throws Exception {
        String subsystemXmlResource = String.format("micrometer-transform-%d_%d_%d.xml",
                version.getMajor(), version.getMinor(), version.getMicro());

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(additionalInitialization)
                .setSubsystemXmlResource(subsystemXmlResource);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(additionalInitialization, controller, version)
                .skipReverseControllerCheck()
                .addMavenResourceURL(getDependencies())
                .dontPersistXml();

        KernelServices services = builder.build();

        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(services.getLegacyServices(version).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, null, false);
    }

    /**
     * Tests rejected transformation of the model from current version into specified version.
     */
    @Test
    public void testRejections() throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(additionalInitialization);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(additionalInitialization, controller, version)
                .skipReverseControllerCheck()
                .addMavenResourceURL(getDependencies())
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(services.isSuccessfulBoot());
        KernelServices legacyServices = services.getLegacyServices(this.version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("micrometer-reject.xml");
        System.out.println(operations);
        FailedOperationTransformationConfig failedOperationTransformationConfig = this.createFailedOperationTransformationConfig();
        ModelTestUtils.checkFailedTransformedBootOperations(services, this.version, operations,
                failedOperationTransformationConfig);
    }

    private FailedOperationTransformationConfig createFailedOperationTransformationConfig() {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, MicrometerConfigurationConstants.NAME);

        if (VERSION_2_0_0.requiresTransformation(this.version)) {
            config.addFailedAttribute(subsystemAddress.append(PrometheusRegistryDefinitionRegistrar.PATH),
                    new FailedOperationTransformationConfig.NewAttributesConfig(PrometheusRegistryDefinitionRegistrar.CONTEXT.getName()));
        }

        return config;
    }

    private String[] getDependencies() {
        if (Objects.requireNonNull(controller) == ModelTestControllerVersion.EAP_XP_5) {
            return new String[]{
                    formatSubsystemArtifact()
            };
        }
        throw new IllegalArgumentException();
    }

    private String formatSubsystemArtifact() {
        return "org.wildfly:wildfly-micrometer:" + controller.getTestControllerVersion() + ".Final";
    }
}
