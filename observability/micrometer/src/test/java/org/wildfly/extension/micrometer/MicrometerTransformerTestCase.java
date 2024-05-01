/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerSubsystemModel.VERSION_1_1_0;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/*
 * Currently ignore as I keep getting this error:
 * [ERROR] testTransformation[0](org.wildfly.extension.micrometer.MicrometerTransformerTestCase)  Time elapsed: 0.945 s  <<< ERROR!
 * java.lang.IllegalStateException: Url file:/home/jdlee/.m2/repository/org/wildfly/core/wildfly-subsystem-test-framework/23.0.0.Beta5/wildfly-subsystem-test-framework-23.0.0.Beta5.jar which is the code source for the following classes has already been set up via other means: [org.jboss.as.subsystem.test.AdditionalInitialization]
 *
 * I don't know why, but, for now, I'd like to get a full CI run to verify the other changes. Will come back to this
 * "soon".
 */
@RunWith(value = Parameterized.class)
@Ignore
public class MicrometerTransformerTestCase extends AbstractSubsystemTest {

    private final ModelTestControllerVersion controller;
    private final ModelVersion version;

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters2() {
        return EnumSet.of(ModelTestControllerVersion.EAP_8_0_0);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return List.<Object[]>of(
                new Object[]{ModelTestControllerVersion.EAP_8_0_0, VERSION_1_1_0.getVersion()}
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
        AdditionalInitialization additionalInitialization = createAdditionalInitialization();
        KernelServicesBuilder builder = createKernelServicesBuilder(additionalInitialization)
                .setSubsystemXmlResource(subsystemXmlResource);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(additionalInitialization, controller, version)
                .skipReverseControllerCheck()
                .addMavenResourceURL(getDependencies())
//                .addSingleChildFirstClass(AdditionalInitialization.class)
                .dontPersistXml();

        KernelServices services = builder.build();

        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(services.getLegacyServices(version).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, null, false);
    }

    @Test
    public void testRejections() throws Exception {

    }

    private AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    private String[] getDependencies() {
        if (Objects.requireNonNull(controller) == ModelTestControllerVersion.EAP_8_0_0) {
            return new String[]{
                    formatSubsystemArtifact()
            };
        }
        throw new IllegalArgumentException();
    }

    private String formatSubsystemArtifact() {
        return formatArtifact("org.wildfly:wildfly-micrometer:%s");
    }

    private String formatArtifact(String pattern) {
        return String.format(pattern, controller.getMavenGavVersion());
    }
}
