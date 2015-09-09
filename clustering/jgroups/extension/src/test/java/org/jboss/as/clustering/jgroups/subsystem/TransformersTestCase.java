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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
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
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
@RunWith(BMUnitRunner.class)
public class TransformersTestCase extends OperationTestCaseBase {

    private static String formatSubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.wildfly:wildfly-clustering-jgroups:%s", version);
    }

    private static String formatLegacySubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.as:jboss-as-clustering-jgroups:%s", version);
    }

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization().require(RequiredCapability.SOCKET_BINDING, "jgroups-tcp", "jgroups-udp", "jgroups-udp-fd", "some-binding", "jgroups-diagnostics", "jgroups-mping", "jgroups-tcp-fd", "jgroups-state-xfr");
    }

    @Test
    public void testTransformerWF800() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.WILDFLY_8_0_0_FINAL;
        testTransformation(JGroupsModel.VERSION_2_0_0, version, formatSubsystemArtifact(version));
    }

    @Test
    public void testTransformerWF810() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.WILDFLY_8_1_0_FINAL;
        testTransformation(JGroupsModel.VERSION_2_0_0, version, formatSubsystemArtifact(version));
    }

    @Test
    public void testTransformerEAP620() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_2_0;
        testTransformation(JGroupsModel.VERSION_1_2_0, version, formatLegacySubsystemArtifact(version));
    }

    @Test
    public void testTransformerEAP630() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_3_0;
        testTransformation(JGroupsModel.VERSION_1_2_0, version, formatLegacySubsystemArtifact(version));
    }

    @Test
    public void testTransformerEAP640() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_4_0;
        testTransformation(JGroupsModel.VERSION_1_3_0, version, formatLegacySubsystemArtifact(version));
    }

    /**
     * Tests transformation of model from current version into specified version.
     *
     * @throws Exception
     */
    private void testTransformation(JGroupsModel model, ModelTestControllerVersion controller, String ... mavenResourceURLs) throws Exception {
        ModelVersion version = model.getVersion();

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(this.createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem-jgroups-transform.xml")
                .enableTransformerAttachmentGrabber();

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(null, controller, version).addMavenResourceURL(mavenResourceURLs).skipReverseControllerCheck();

        KernelServices services = builder.build();

        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(services.getLegacyServices(version).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version);

        // Test properties operations
        propertiesMapOperationsTest(services, version);
    }

    private void propertiesMapOperationsTest(KernelServices services, ModelVersion version) throws Exception {

        // n.b. only compare actual results since they differ in reload-required
        List<ModelNode> results;

        // Check operations on /transport=*
        executeOpInBothControllersWithAttachments(services, version, getTransportUndefinePropertiesOperation("maximal", "TCP"));

        executeOpInBothControllersWithAttachments(services, version, getTransportPutPropertyOperation("maximal", "TCP", "tcp_nodelay", "true"));
        results = executeOpInBothControllers(services, version, getTransportGetPropertyOperation("maximal", "TCP", "tcp_nodelay"));
        Assert.assertEquals("Failed to read property value back following a put", results.get(0).get(ModelDescriptionConstants.RESULT), results.get(1).get(ModelDescriptionConstants.RESULT));

        executeOpInBothControllersWithAttachments(services, version, getTransportRemovePropertyOperation("maximal", "TCP", "tcp_nodelay"));
        executeOpInBothControllersWithAttachments(services, version, getTransportPutPropertyOperation("maximal", "TCP", "tcp_nodelay", "true"));
        results = executeOpInBothControllers(services, version, getTransportGetPropertyOperation("maximal", "TCP", "tcp_nodelay"));
        Assert.assertEquals("Failed to read property value following a remove and a put", results.get(0).get(ModelDescriptionConstants.RESULT), results.get(1).get(ModelDescriptionConstants.RESULT));

        executeOpInBothControllersWithAttachments(services, version, getTransportClearPropertiesOperation("maximal", "TCP"));

        // Check operations on /protocol=*
        executeOpInBothControllersWithAttachments(services, version, getProtocolUndefinePropertiesOperation("maximal", "MPING"));

        executeOpInBothControllersWithAttachments(services, version, getProtocolPutPropertyOperation("maximal", "MPING", "send_on_all_interfaces", "true"));
        results = executeOpInBothControllers(services, version, getProtocolGetPropertyOperation("maximal", "MPING", "send_on_all_interfaces"));
        Assert.assertEquals(results.get(0).get(ModelDescriptionConstants.RESULT), results.get(1).get(ModelDescriptionConstants.RESULT));

        executeOpInBothControllersWithAttachments(services, version, getProtocolRemovePropertyOperation("maximal", "MPING", "send_on_all_interfaces"));
        executeOpInBothControllersWithAttachments(services, version, getProtocolPutPropertyOperation("maximal", "MPING", "send_on_all_interfaces", "true"));
        results = executeOpInBothControllers(services, version, getProtocolGetPropertyOperation("maximal", "MPING", "send_on_all_interfaces"));
        Assert.assertEquals(results.get(0).get(ModelDescriptionConstants.RESULT), results.get(1).get(ModelDescriptionConstants.RESULT));

        executeOpInBothControllersWithAttachments(services, version, getProtocolClearPropertiesOperation("maximal", "MPING"));

// org.jboss.as.subsystem.test.MainKernelServicesImpl does not behave correctly:
//
//        In a real domain the operation and transformation will happen in one call to the controller,
//        in the subsystem test, the attachment part was bolted on and we do two calls,
//        which is why we need to test this currently in the mixed-domain part.
//
//        // Check composite operation
//        final ModelNode composite = new ModelNode();
//        composite.get(OP).set(COMPOSITE);
//        composite.get(OP_ADDR).setEmptyList();
//        composite.get(STEPS).add(getProtocolPutPropertyOperation("maximal", "MPING", "send_on_all_interfaces", "false"));
//        composite.get(STEPS).add(getProtocolPutPropertyOperation("maximal", "MPING", "receive_on_all_interfaces", "false"));
//        executeOpInBothControllersWithAttachments(services, version, composite);
//
//        // Reread values back
//        results = executeOpInBothControllers(services, version, getProtocolGetPropertyOperation("maximal", "MPING", "send_on_all_interfaces"));
//        Assert.assertEquals(results.get(0).get(ModelDescriptionConstants.RESULT), results.get(1).get(ModelDescriptionConstants.RESULT));
//
//        results = executeOpInBothControllers(services, version, getProtocolGetPropertyOperation("maximal", "MPING", "receive_on_all_interfaces"));
//        Assert.assertEquals(results.get(0).get(ModelDescriptionConstants.RESULT), results.get(1).get(ModelDescriptionConstants.RESULT));

    }

    /**
     * Tests resolution of property expressions during performRuntime()
     *
     * This test uses Byteman to inject code into AbstractAddStepHandler.performRuntime() to
     * resolve the value of an expression and check that expression resolution is working as expected.
     *
     * The test is currently broken due to an outstanding class loading problem with Byteman, but it is included
     * here for re-enabling when the issue is resolved.
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
        ModelNode operation = getTransportPropertyAddOperation("maximal", "TCP", "bundler_type", "${the_bundler_type:new}");

        // perform operation on the 1.1.1 model
        ModelNode mainResult = services.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());
    }

    @Test
    public void testRejectionsWF800() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.WILDFLY_8_0_0_FINAL;
        this.testRejections(JGroupsModel.VERSION_2_0_0, version, formatSubsystemArtifact(version));
    }

    @Test
    public void testRejectionsWF810() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.WILDFLY_8_1_0_FINAL;
        this.testRejections(JGroupsModel.VERSION_2_0_0, version, formatSubsystemArtifact(version));
    }

    @Test
    public void testRejectionsEAP620() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_2_0;
        this.testRejections(JGroupsModel.VERSION_1_2_0, version, formatLegacySubsystemArtifact(version));
    }

    @Test
    public void testRejectionsEAP630() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_3_0;
        this.testRejections(JGroupsModel.VERSION_1_2_0, version, formatLegacySubsystemArtifact(version));
    }

    @Test
    public void testRejectionsEAP640() throws Exception {
        ModelTestControllerVersion version = ModelTestControllerVersion.EAP_6_4_0;
        this.testRejections(JGroupsModel.VERSION_1_3_0, version, formatLegacySubsystemArtifact(version));
    }

    private void testRejections(JGroupsModel model, ModelTestControllerVersion controller, String... dependencies) throws Exception {
        ModelVersion version = model.getVersion();

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(this.createAdditionalInitialization())
                .enableTransformerAttachmentGrabber();

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(null, controller, version)
                .addMavenResourceURL(dependencies)
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(services.isSuccessfulBoot());
        KernelServices legacyServices = services.getLegacyServices(version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("subsystem-jgroups-transform-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, version, operations, createFailedOperationTransformationConfig(version));
    }

    private static FailedOperationTransformationConfig createFailedOperationTransformationConfig(ModelVersion version) {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Channel resource in a typical configuration would be not rejected, but since we don't have infinispan subsystem setup (because
            // that would create a cyclical dependency) it has to be rejected in this subsystem test
            config.addFailedAttribute(subsystemAddress.append(ChannelResourceDefinition.WILDCARD_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            config.addFailedAttribute(subsystemAddress.append(StackResourceDefinition.WILDCARD_PATH).append(TransportResourceDefinition.WILDCARD_PATH).append(ThreadPoolResourceDefinition.WILDCARD_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }

        if (JGroupsModel.VERSION_2_0_0.requiresTransformation(version)) {
            PathAddress stackAddress = subsystemAddress.append(StackResourceDefinition.WILDCARD_PATH);
            PathAddress relayAddress = stackAddress.append(RelayResourceDefinition.PATH);
            config.addFailedAttribute(relayAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
            config.addFailedAttribute(relayAddress.append(RemoteSiteResourceDefinition.WILDCARD_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }

        return config;
    }
}
