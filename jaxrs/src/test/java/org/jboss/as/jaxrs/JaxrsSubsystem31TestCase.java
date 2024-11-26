/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

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

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JaxrsSubsystem31TestCase extends AbstractSubsystemBaseTest {

    public JaxrsSubsystem31TestCase() {
        super(JaxrsExtension.SUBSYSTEM_NAME, new JaxrsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jaxrs-3.1.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jaxrs_3_1.xsd";
    }

    @Override
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null);
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("jaxrs-3.1-expressions.xml",true);
    }

    @Test
    public void testRejectingTransformersEAP74() throws Exception {
        FailedOperationTransformationConfig transformationConfig = new FailedOperationTransformationConfig();

        transformationConfig.addFailedAttribute(PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH),
                new FailedOperationTransformationConfig.NewAttributesConfig(JaxrsAttribute.TRACING_THRESHOLD,
                        JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.RESTEASY_PATCHFILTER_DISABLED));

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

        List<ModelNode> operations = builder.parseXmlResource("jaxrs-3.1.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(kernelServices, subsystemModelVersion, operations, transformationConfig);
    }
}
