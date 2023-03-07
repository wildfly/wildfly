/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
public class JaxrsSubsystem30TestCase extends AbstractSubsystemBaseTest {

    public JaxrsSubsystem30TestCase() {
        super(JaxrsExtension.SUBSYSTEM_NAME, new JaxrsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jaxrs.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jaxrs_3_0.xsd";
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
                new FailedOperationTransformationConfig.NewAttributesConfig(JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.TRACING_THRESHOLD));

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

        List<ModelNode> operations = builder.parseXmlResource("jaxrs.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(kernelServices, subsystemModelVersion, operations, transformationConfig);
    }
}
