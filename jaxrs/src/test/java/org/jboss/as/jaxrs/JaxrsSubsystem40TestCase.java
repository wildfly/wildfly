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
import org.junit.Ignore;
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
        return readResource("jaxrs.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jaxrs_4_0.xsd";
    }

    @Override
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null);
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("jaxrs-expressions.xml");
    }

    @Test
    public void testRejectingTransformersEAP74() throws Exception {
        FailedOperationTransformationConfig transformationConfig = new FailedOperationTransformationConfig();

        transformationConfig.addFailedAttribute(PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH),
                new FailedOperationTransformationConfig.NewAttributesConfig(
                        JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.TRACING_THRESHOLD,
                        JaxrsAttribute.RESTEASY_MATCH_CACHE_ENABLED,
                        JaxrsAttribute.RESTEASY_MATCH_CACHE_SIZE,
                        JaxrsAttribute.RESTEASY_PATCH_FILTER_DISABLED,
                        JaxrsAttribute.RESTEASY_PATCH_FILTER_LEGACY,
                        JaxrsAttribute.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR,
                        JaxrsAttribute.RESTEASY_PROXY_IMPLEMENT_ALL_INTERFACES
                        ));

        testRejectingTransformers74(transformationConfig, ModelTestControllerVersion.EAP_7_4_0);
    }

    @Test
    @Ignore("temporary: https://issues.redhat.com/browse/WFCORE-6700")
    public void testRejectingTransformersEAP80() throws Exception {
        FailedOperationTransformationConfig transformationConfig = new FailedOperationTransformationConfig();

        transformationConfig.addFailedAttribute(PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH),
                new FailedOperationTransformationConfig.NewAttributesConfig(
                        JaxrsAttribute.RESTEASY_MATCH_CACHE_ENABLED,
                        JaxrsAttribute.RESTEASY_MATCH_CACHE_SIZE,
                        JaxrsAttribute.RESTEASY_PATCH_FILTER_DISABLED,
                        JaxrsAttribute.RESTEASY_PATCH_FILTER_LEGACY,
                        JaxrsAttribute.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR,
                        JaxrsAttribute.RESTEASY_PROXY_IMPLEMENT_ALL_INTERFACES
                        ));

        testRejectingTransformers80(transformationConfig, ModelTestControllerVersion.EAP_8_0_0);
    }

    private void testRejectingTransformers74(FailedOperationTransformationConfig transformationConfig, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion subsystemModelVersion = controllerVersion.getSubsystemModelVersion(JaxrsExtension.SUBSYSTEM_NAME);

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        LegacyKernelServicesInitializer kernelServicesInitializer = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, subsystemModelVersion)
                .addMavenResourceURL("org.wildfly.core:wildfly-threads:" + controllerVersion.getCoreVersion())
                .dontPersistXml();
        kernelServicesInitializer.addMavenResourceURL("org.wildfly:wildfly-jaxrs:26.0.0.Final");
        KernelServices kernelServices = builder.build();
        assertTrue(kernelServices.isSuccessfulBoot());
        assertTrue(kernelServices.getLegacyServices(subsystemModelVersion).isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("jaxrs.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(kernelServices, subsystemModelVersion, operations, transformationConfig);
    }

    private void testRejectingTransformers80(FailedOperationTransformationConfig transformationConfig, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion subsystemModelVersion = controllerVersion.getSubsystemModelVersion(JaxrsExtension.SUBSYSTEM_NAME);

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        LegacyKernelServicesInitializer kernelServicesInitializer = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, subsystemModelVersion)
                .addMavenResourceURL("org.wildfly.core:wildfly-threads:" + controllerVersion.getCoreVersion())
                .dontPersistXml();
        kernelServicesInitializer.addMavenResourceURL("org.wildfly:wildfly-jaxrs:31.0.0.Final");
        KernelServices kernelServices = builder.build();
        assertTrue(kernelServices.isSuccessfulBoot());
        assertTrue(kernelServices.getLegacyServices(subsystemModelVersion).isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("jaxrs.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(kernelServices, subsystemModelVersion, operations, transformationConfig);
    }
}
