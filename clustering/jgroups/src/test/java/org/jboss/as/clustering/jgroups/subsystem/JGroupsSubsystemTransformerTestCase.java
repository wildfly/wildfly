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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import junit.framework.Assert;

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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases for transformers used in the JGroups subsystem.
 *
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */

@RunWith(BMUnitRunner.class)
public class JGroupsSubsystemTransformerTestCase extends OperationTestCaseBase {

    protected String getSubsystemXml() throws IOException {
        return "jgroups-transformer_1_1.xml";
    }

    @Test
    public void testTransformerAS712() throws Exception {
        testTransformer_1_1_0("7.1.2.Final", ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformerAS713() throws Exception {
        testTransformer_1_1_0("7.1.3.Final", ModelTestControllerVersion.V7_1_3_FINAL);
    }

    /**
     * Tests transformation of model from 1.1.1 version into 1.1.0 version.
     *
     * This test does not pass because of an error in the 1.1.0 model:
     * The model entry 'protocols' in /subsystem=jgroups/stack=* is used
     * to store an ordered list of protocol names, but it is not registered
     * as an attribute, where as it is registered in the 1.1.1 model.
     * This breaks the test.
     *
     * @throws Exception
     */
    private void testTransformer_1_1_0(String mavenVersion, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion version = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
                .addMavenResourceURL("org.jboss.as:jboss-as-clustering-jgroups:" + mavenVersion);

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(version).isSuccessfulBoot());
        checkSubsystemModelTransformation(mainServices, version);
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
            action="traceln(\"resolved value = \" + org.jboss.as.clustering.jgroups.subsystem.PropertyResource.VALUE.resolveModelAttribute(context,model))")
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
    public void testRejectExpressionsAS712() throws Exception {
        testRejectExpressions_1_1_0("org.jboss.as:jboss-as-clustering-jgroups:7.1.2.Final", ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        testRejectExpressions_1_1_0("org.jboss.as:jboss-as-clustering-jgroups:7.1.3.Final", ModelTestControllerVersion.V7_1_3_FINAL);
    }

    /**
     * Tests rejection of expressions in 1.1.0 model.
     *
     * @throws Exception
     */
    private void testRejectExpressions_1_1_0(String mavenGAV, ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_1_0)
            .addMavenResourceURL(mavenGAV);

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
                builder.parseXmlResource("subsystem-jgroups-test.xml"),
                new FailedOperationTransformationConfig()
                    .addFailedAttribute(
                            subsystemAddress.append(PathElement.pathElement("stack"))
                                    .append(PathElement.pathElement("transport")),
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(ModelKeys.SHARED))
                    .addFailedAttribute(
                            subsystemAddress.append(PathElement.pathElement("stack"))
                                .append(PathElement.pathElement("transport"))
                                .append("property"),
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(VALUE))
                    .addFailedAttribute(
                            subsystemAddress.append(PathElement.pathElement("stack"))
                                .append(PathElement.pathElement("protocol"))
                                .append("property"),
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(VALUE)));

    }

}
