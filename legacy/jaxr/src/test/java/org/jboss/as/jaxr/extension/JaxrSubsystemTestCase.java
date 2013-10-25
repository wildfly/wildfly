/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JaxrSubsystemTestCase extends AbstractSubsystemBaseTest {

    public JaxrSubsystemTestCase() {
        super(JAXRExtension.SUBSYSTEM_NAME, new JAXRExtension());
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("xsd1_1full.xml");
    }

    @Test
    public void testFullExpressions() throws Exception {
        standardSubsystemTest("xsd1_1expressions.xml");
    }

    @Test
    public void testSubsystem1_0() throws Exception {
        standardSubsystemTest("xsd1_0.xml", false);
    }

    /**
     * Tests transformation of model from 1.2.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    @Test
    public void testTransformerAS712() throws Exception {
        testTransformers1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    /**
     * Tests transformation of model from 1.2.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    @Test
    public void testTransformerAS713() throws Exception {
        testTransformers1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformers1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = "xsd1_1full.xml";   //This has no expressions not understood by 1.1.0
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-jaxr:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.jaxr.extension.JAXRSubsystemExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Test
    public void testRejectExpressionsAS712() throws Exception {
        testRejectExpressions1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        testRejectExpressions1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testRejectExpressions1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, version_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-jaxr:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.jaxr.extension.JAXRSubsystemExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("xsd1_1expressions.xml");

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1_0, xmlOps,
                new FailedOperationTransformationConfig()
                .addFailedAttribute(PathAddress.pathAddress(JAXRExtension.SUBSYSTEM_PATH),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(JAXRSubsystemRootResource.CONNECTION_FACTORY_ATTRIBUTE))
        );
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        // A minimal config
        return "<subsystem xmlns=\"urn:jboss:domain:jaxr:1.1\">" +
        "<connection-factory jndi-name=\"java:jboss/jaxr/ConnectionFactory\"/>" +
        "</subsystem>";
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {

            @Override
            protected ProcessType getProcessType() {
                return ProcessType.HOST_CONTROLLER;
            }

            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }
        };
    }

    @Override
    protected void validateDescribeOperation(KernelServices hc, AdditionalInitialization serverInit, ModelNode expectedModel) throws Exception {
        final ModelNode operation = createDescribeOperation();
        final ModelNode result = hc.executeOperation(operation);
        Assert.assertTrue("The subsystem describe operation must fail",
                result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
    }


}
