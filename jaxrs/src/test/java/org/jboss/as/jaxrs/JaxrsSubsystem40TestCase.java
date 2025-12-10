/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JaxrsSubsystem40TestCase extends AbstractSubsystemBaseTest {

    public JaxrsSubsystem40TestCase() {
        super(JaxrsExtension.SUBSYSTEM_NAME, new JaxrsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jaxrs-4.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/jboss-as-jaxrs_4_0.xsd";
    }

    @Override
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null, false);
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("jaxrs-expressions-4.0.xml", false);
    }

    @Test
    public void testRejectingTransformersEAP80() throws Exception {
        FailedOperationTransformationConfig transformationConfig = new FailedOperationTransformationConfig();

        transformationConfig.addFailedAttribute(PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH),
                new FailedOperationTransformationConfig.NewAttributesConfig(JaxrsAttribute.RESTEASY_PATCHFILTER_DISABLED));

        testRejectingTransformersEAP80(transformationConfig, ModelTestControllerVersion.EAP_8_0_0);
    }

    @Test
    public void testRejectingTransformersEAP74() throws Exception {
        FailedOperationTransformationConfig transformationConfig = new FailedOperationTransformationConfig();

        transformationConfig.addFailedAttribute(PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH),
                new FailedOperationTransformationConfig.NewAttributesConfig(JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.TRACING_THRESHOLD, JaxrsAttribute.RESTEASY_PATCHFILTER_DISABLED));

        testRejectingTransformers(transformationConfig, ModelTestControllerVersion.EAP_7_4_0);
    }

    private void testRejectingTransformers(FailedOperationTransformationConfig transformationConfig, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion subsystemModelVersion = controllerVersion.getSubsystemModelVersion(JaxrsExtension.SUBSYSTEM_NAME);

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        LegacyKernelServicesInitializer kernelServicesInitializer = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, subsystemModelVersion)
                .addMavenResourceURL("org.wildfly.core:wildfly-threads:" + controllerVersion.getCoreVersion())
                .dontPersistXml();
        kernelServicesInitializer.addMavenResourceURL("org.wildfly:wildfly-jaxrs:26.0.0.Final");
        KernelServices kernelServices = builder.build();
        assertTrue(kernelServices.isSuccessfulBoot());
        assertTrue(kernelServices.getLegacyServices(subsystemModelVersion).isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("jaxrs-4.0.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(kernelServices, subsystemModelVersion, operations, transformationConfig);
    }

    private void testRejectingTransformersEAP80(FailedOperationTransformationConfig transformationConfig, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion subsystemModelVersion = controllerVersion.getSubsystemModelVersion(JaxrsExtension.SUBSYSTEM_NAME);

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, subsystemModelVersion)
                .addMavenResourceURL(controllerVersion.createCoreGAV("wildfly-threads"))
                .addMavenResourceURL(controllerVersion.createGAV("wildfly-jaxrs"))
                .dontPersistXml();
        KernelServices kernelServices = builder.build();
        assertTrue(kernelServices.isSuccessfulBoot());
        assertTrue(kernelServices.getLegacyServices(subsystemModelVersion).isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("jaxrs-4.0.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(kernelServices, subsystemModelVersion, operations, transformationConfig);
    }
}
