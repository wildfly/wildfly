/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases for transformers used in the JGroups subsystem.
 *
 * AS version / model version / schema version overview
 * 7.1.1 / 1.1.0 / 1_1
 * 7.1.2 / 1.1.0 / 1_1
 * 7.1.3 / 1.1.0 / 1_1
 * 7.2.0 / 1.2.0 / 1_1
 * 8.0.0 / 2.0.0 / 2_0
 *
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
@RunWith(BMUnitRunner.class)
public class TransformersTestCase extends OperationTestCaseBase {

    @Test
    public void testTransformerAS712() throws Exception {
        testTransformer_1_1_0(
                ModelTestControllerVersion.V7_1_2_FINAL,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.V7_1_2_FINAL.getMavenGavVersion()
        );
    }

    @Test
    public void testTransformerAS713() throws Exception {
        testTransformer_1_1_0(
                ModelTestControllerVersion.V7_1_3_FINAL,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.V7_1_3_FINAL.getMavenGavVersion()
        );
    }

    @Test
    public void testTransformerAS720() throws Exception {
        testTransformer_1_2_0(
                ModelTestControllerVersion.V7_2_0_FINAL,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.V7_2_0_FINAL.getMavenGavVersion()
                );
    }

    @Test
    public void testTransformerEAP600() throws Exception {
        testTransformer_1_1_0(
                ModelTestControllerVersion.EAP_6_0_0,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.EAP_6_0_0.getMavenGavVersion()
        );
    }

    @Test
    public void testTransformerEAP601() throws Exception {
        testTransformer_1_1_0(
                ModelTestControllerVersion.EAP_6_0_1,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.EAP_6_0_1.getMavenGavVersion()
        );
    }

    @Test
    public void testTransformerEAP610() throws Exception {
        testTransformer_1_2_0(
                ModelTestControllerVersion.EAP_6_1_0,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.EAP_6_1_0.getMavenGavVersion()
                );
    }

    @Test
    public void testTransformerEAP611() throws Exception {
        testTransformer_1_2_0(
                ModelTestControllerVersion.EAP_6_1_1,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.EAP_6_1_1.getMavenGavVersion()
                );
    }

    /**
     * Tests transformation of model from 2.0.0 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testTransformer_1_1_0(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version110 = ModelVersion.create(1,1,0);
        String subsystemXml = readResource("subsystem-jgroups-transform-2_0.xml");

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version110)
                .addMavenResourceURL(mavenResourceURLs);

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(version110).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(mainServices, version110);

        // 1.1.0 API specific checks:
        // - check description here
    }

    /**
     * Tests transformation of model from 2.0.0 version into 1.2.0 version.
     *
     * @throws Exception
     */
    private void testTransformer_1_2_0(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version120 = ModelVersion.create(1,2,0);
        String subsystemXml = readResource("subsystem-jgroups-transform-2_0.xml");

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version120)
                .addMavenResourceURL(mavenResourceURLs);

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(version120).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(mainServices, version120);

        // 1.2.0 API specific checks:
        // - check description here
    }

    /**
     * Tests resolution of property expressions during performRuntime()
     *
     * This test uses Byteman to inject code into AbstractAddStepHandler.performRuntime() to
     * resolve the value of an expression and check that expression resolution is working as expected.
     *
     * The test is currently broken due to an outstanding class loading problem with Byteman, but it is included
     * here for re-enabling when the issue is resolved.
     *
     * @throws Exception
     */
    @Ignore
    @Test
    @BMRule(name="Test support for expression resolution",
            targetClass="^org.jboss.as.controller.AbstractAddStepHandler",
            targetMethod="performRuntime",
            targetLocation="AT ENTRY",
            binding="context:OperationContext = $1; operation:ModelNode = $2; model:ModelNode = $3",
            condition="operation.hasDefined(\"name\") AND operation.hasDefined(\"value\")",
            action="traceln(\"resolved value = \" + org.jboss.as.clustering.jgroups.subsystem.PropertyResourceDefinition.VALUE.resolveModelAttribute(context,model))")
    public void testProtocolStackPropertyResolve() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml =  getSubsystemXml() ;
        KernelServices services = createKernelServicesBuilder(null).setSubsystemXmlResource(subsystemXml).build();

        // set a property to have an expression and let Byteman intercept the performRuntime call

        // build an ADD command to add a transport property using expression value
        ModelNode operation = getTransportPropertyAddOperation("maximal", "bundler_type", "${the_bundler_type:new}");

        // perform operation on the 1.1.1 model
        ModelNode mainResult = services.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());
    }

    @Test
    public void testRejectionsAS712() throws Exception {
        testRejections_1_1_0(
                ModelTestControllerVersion.V7_1_2_FINAL,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.V7_1_2_FINAL.getMavenGavVersion()
                );
    }

    @Test
    public void testRejectionsAS713() throws Exception {
        testRejections_1_1_0(
                ModelTestControllerVersion.V7_1_3_FINAL,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.V7_1_3_FINAL.getMavenGavVersion()
                );
    }

    @Test
    public void testRejectionsAS720() throws Exception {
        testRejections_1_2_0(
                ModelTestControllerVersion.V7_2_0_FINAL,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.V7_2_0_FINAL.getMavenGavVersion()
                );
    }

    @Test
    public void testRejectionsEAP600() throws Exception {
        testRejections_1_1_0(
                ModelTestControllerVersion.EAP_6_0_0,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.EAP_6_0_0.getMavenGavVersion()
                );
    }

    @Test
    public void testRejectionsEAP601() throws Exception {
        testRejections_1_1_0(
                ModelTestControllerVersion.EAP_6_0_1,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.EAP_6_0_1.getMavenGavVersion()
                );
    }

    @Test
    public void testRejectionsEAP610() throws Exception {
        testRejections_1_2_0(
                ModelTestControllerVersion.EAP_6_1_0,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.EAP_6_1_0.getMavenGavVersion()
                );
    }

    @Test
    public void testRejectionsEAP611() throws Exception {
        testRejections_1_2_0(
                ModelTestControllerVersion.EAP_6_1_1,
                "org.jboss.as:jboss-as-clustering-jgroups:" + ModelTestControllerVersion.EAP_6_1_1.getMavenGavVersion()
                );
    }

    /**
     * Tests rejection of resources / attributes / operations in 1.1.0 model.
     *
     * @throws Exception
     */
    private void testRejections_1_1_0(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_1_0)
                .addMavenResourceURL(mavenResourceURLs)
                //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
                //which is strange since it should be loading it all from the current jboss modules
                //Also this works in several other tests
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        //Use the real xml with expressions for testing all the attributes
        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME));
        ModelTestUtils.checkFailedTransformedBootOperations(
                mainServices,
                version_1_1_0,
                builder.parseXmlResource("subsystem-jgroups-2_0.xml"),
                new FailedOperationTransformationConfig()
                        // expect certain rejected expressions
                        .addFailedAttribute(
                                subsystemAddress.append(PathElement.pathElement("stack"))
                                        .append(PathElement.pathElement("transport", "TRANSPORT")),
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(ModelKeys.SHARED))
                        .addFailedAttribute(
                                subsystemAddress.append(PathElement.pathElement("stack"))
                                        .append(PathElement.pathElement("transport", "TRANSPORT"))
                                        .append("property"),
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(PathElement.pathElement("stack"))
                                        .append(PathElement.pathElement("protocol"))
                                        .append("property"),
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(VALUE))
                        // expect rejection of relay and child
                        .addFailedAttribute(
                                subsystemAddress.append("stack").append("relay", "RELAY"),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append("stack").append("relay", "RELAY").append("remote-site"),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
        );
    }

    /**
     * Tests rejection of resources / attributes / operations in 1.2.0 model.
     *
     * @throws Exception
     */
    private void testRejections_1_2_0(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version_1_2_0 = ModelVersion.create(1, 2, 0);

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_2_0)
                .addMavenResourceURL(mavenResourceURLs)
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_2_0);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // Use the real xml with expressions for testing all the attributes
        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME));
        ModelTestUtils.checkFailedTransformedBootOperations(
                mainServices,
                version_1_2_0,
                builder.parseXmlResource("subsystem-jgroups-transform-2_0-reject.xml"),
                new FailedOperationTransformationConfig()
                        // expect rejection of relay and child
                        .addFailedAttribute(
                                subsystemAddress.append("stack").append("relay", "RELAY"),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append("stack").append("relay", "RELAY").append("remote-site"),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
        );
    }
}
