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

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;

/**
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

    //commented out as current version of the subsystem model is the same as the EAP 6.2+
    /*@Test
    public void testTransformerEAP620() throws Exception {
        testTransformers1_2_0(ModelTestControllerVersion.EAP_6_2_0);
    }

    @Test
    public void testTransformerEAP630() throws Exception {
        testTransformers1_2_0(ModelTestControllerVersion.EAP_6_3_0);
    }

    @Test
    public void testTransformerEAP640() throws Exception {
        testTransformers1_2_0(ModelTestControllerVersion.EAP_6_4_0);
    }

    private void testTransformers1_2_0(ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = "xsd1_1full.xml";   //This has no expressions not understood by 1.1.0
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-jaxr:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Test
    public void testRejectExpressionsEAP620() throws Exception {
        testRejectExpressions1_2_0(ModelTestControllerVersion.EAP_6_2_0);
    }

    @Test
    public void testRejectExpressionsEAP630() throws Exception {
        testRejectExpressions1_2_0(ModelTestControllerVersion.EAP_6_4_0);
    }

    @Test
    public void testRejectExpressionsEAP640() throws Exception {
        testRejectExpressions1_2_0(ModelTestControllerVersion.EAP_6_4_0);
    }


    private void testRejectExpressions1_2_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        ModelVersion version_1_2_0 = ModelVersion.create(1, 2, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_2_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-jaxr:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_2_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("xsd1_1expressions.xml");

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_2_0, xmlOps, new FailedOperationTransformationConfig()); //noting to reject at this point
    }
*/
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

    // TODO WFCORE-1353 means this doesn't have to always fail now; consider just deleting this
//    @Override
//    protected void validateDescribeOperation(KernelServices hc, AdditionalInitialization serverInit, ModelNode expectedModel) throws Exception {
//        final ModelNode operation = createDescribeOperation();
//        final ModelNode result = hc.executeOperation(operation);
//        Assert.assertTrue("The subsystem describe operation must fail",
//                result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
//    }


}
