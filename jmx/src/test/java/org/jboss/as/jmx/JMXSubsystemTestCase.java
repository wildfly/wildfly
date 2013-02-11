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
package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.remoting.EndpointService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.junit.Test;
import org.xnio.OptionMap;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JMXSubsystemTestCase extends AbstractSubsystemTest {

    private static final String LAUNCH_TYPE = "launch-type";
    private static final String TYPE_STANDALONE = "STANDALONE";

    public JMXSubsystemTestCase() {
        super(JMXExtension.SUBSYSTEM_NAME, new JMXExtension());
    }

    @Test
    public void testParseEmptySubsystem() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "</subsystem>";
        List<ModelNode> operations = super.parse(subsystemXml);

        ///Check that we have the expected number of operations
        Assert.assertEquals(1, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(JMXExtension.SUBSYSTEM_NAME, element.getValue());
    }

    @Test
    public void testParseSubsystemWithBadChild() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "   <invalid/>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad child");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testParseSubsystemWithBadAttribute() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\" bad=\"very_bad\">" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad attribute");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testParseSubsystemWithConnector() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <remoting-connector use-management-endpoint=\"false\" />" +
                "</subsystem>";
        List<ModelNode> operations = super.parse(subsystemXml);

        ///Check that we have the expected number of operations
        Assert.assertEquals(2, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        assertJmxSubsystemAddress(addSubsystem.get(OP_ADDR));

        ModelNode addConnector = operations.get(1);
        Assert.assertEquals(ADD, addConnector.get(OP).asString());
        assertJmxConnectorAddress(addConnector.get(OP_ADDR));
    }

    @Test
    public void testParseSubsystemWithTwoConnectors() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "<remoting-connector/>" +
                "<remoting-connector/>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed second connector");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testParseSubsystemWithBadConnectorAttribute() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "<remoting-connector bad=\"verybad\"/>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad attribute");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testInstallIntoController() throws Exception {
        //Parse the subsystem xml and install into the controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "<remoting-connector/>" +
                "</subsystem>";
        KernelServices services = createKernelServicesBuilder(new BaseAdditionalInitalization())
                .setSubsystemXml(subsystemXml)
                .build();

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(JMXExtension.SUBSYSTEM_NAME));

        //Make sure that we can connect to the MBean server
        int port = 12345;
        String urlString = System.getProperty("jmx.service.url",
            "service:jmx:remoting-jmx://localhost:" + port);
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);

        MBeanServerConnection connection = null;
        long end = System.currentTimeMillis() + 10000;
        do {
            try {
                connection = JMXConnectorFactory.connect(serviceURL, null).getMBeanServerConnection();
            } catch (Exception e) {
                e.printStackTrace();
                Thread.sleep(500);
            }
        } while (connection == null && System.currentTimeMillis() < end);
        Assert.assertNotNull(connection);
        connection.getMBeanCount();

        super.assertRemoveSubsystemResources(services);
    }



    @Test
    public void testParseAndMarshalModel1_0() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.JMX_1_0.getUriString() + "\">" +
                "    <jmx-connector registry-binding=\"registry1\" server-binding=\"server1\" />" +
                "</subsystem>";
        String finishedSubsystemXml =
                        "<subsystem xmlns=\"" + Namespace.JMX_1_0.getUriString() + "\"/>";
        AdditionalInitialization additionalInit = new BaseAdditionalInitalization();

        KernelServices servicesA = createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        Assert.assertTrue(marshalled.contains(Namespace.CURRENT.getUriString()));

        compareXml(null, finishedSubsystemXml, marshalled, true);

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testParseAndMarshalModel1_1WithShowModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.JMX_1_1.getUriString() + "\">" +
                "<show-model value=\"true\"/>" +
                "</subsystem>";

        String finishedXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <expose-resolved-model proper-property-format=\"false\"/>" +
                "</subsystem>";

        AdditionalInitialization additionalInit = new BaseAdditionalInitalization();

        KernelServices servicesA = createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        Assert.assertTrue(marshalled.contains(Namespace.CURRENT.getUriString()));
        compareXml(null, finishedXml, marshalled, true);

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }


    @Test
    public void testParseAndMarshalModelWithRemoteConnectorRef1_1() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.JMX_1_1.getUriString() + "\">" +
                "<remoting-connector/> " +
                "</subsystem>";


        AdditionalInitialization additionalInit = new BaseAdditionalInitalization();

        KernelServices servicesA = createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        compareXml(null, subsystemXml, marshalled, true);

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testParseAndMarshalModel1_1() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.JMX_1_1.getUriString() + "\">" +
                "<show-model value=\"true\"/>" +
                "<remoting-connector/>" +
                "</subsystem>";

        String finishedXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "    <expose-resolved-model proper-property-format=\"false\"/>" +
                "    <remoting-connector/>" +
                "</subsystem>";

        AdditionalInitialization additionalInit = new BaseAdditionalInitalization();

        KernelServices servicesA = createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        Assert.assertTrue(marshalled.contains(Namespace.CURRENT.getUriString()));
        compareXml(null, finishedXml, marshalled, true);

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testParseAndMarshalModel1_2WithShowModels() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.JMX_1_2.getUriString() + "\">" +
                "   <expose-resolved-model domain-name=\"jboss.RESOLVED\"/>" +
                "   <expose-expression-model domain-name=\"jboss.EXPRESSION\"/>" +
                "</subsystem>";

        AdditionalInitialization additionalInit = new BaseAdditionalInitalization();

        KernelServices servicesA = createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        Assert.assertTrue(marshalled.contains(Namespace.CURRENT.getUriString()));
        compareXml(null, subsystemXml, marshalled, true);

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testParseAndMarshalModel1_2WithShowModelsAndOldPropertyFormat() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.JMX_1_2.getUriString() + "\">" +
                "   <expose-resolved-model domain-name=\"jboss.RESOLVED\" proper-property-format=\"false\"/>" +
                "   <expose-expression-model domain-name=\"jboss.EXPRESSION\"/>" +
                "</subsystem>";

        AdditionalInitialization additionalInit = new BaseAdditionalInitalization();

        KernelServices servicesA = createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        Assert.assertTrue(modelA.get(SUBSYSTEM, "jmx", CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED).hasDefined(CommonAttributes.PROPER_PROPERTY_FORMAT));
        Assert.assertFalse(modelA.get(SUBSYSTEM, "jmx", CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED, CommonAttributes.PROPER_PROPERTY_FORMAT).asBoolean());
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        Assert.assertTrue(marshalled.contains(Namespace.CURRENT.getUriString()));
        compareXml(null, subsystemXml, marshalled, true);

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testDescribeHandler() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "   <expose-resolved-model domain-name=\"jboss.RESOLVED\"/>" +
                "   <expose-expression-model domain-name=\"jboss.EXPRESSION\"/>" +
                "    <remoting-connector />" +
                "</subsystem>";
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();
        //Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
                PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();
        servicesA.shutdown();

        Assert.assertEquals(4, operations.size());


        //Install the describe options from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(null).setBootOperations(operations).build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Test
    public void testShowModelAlias() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\"/>";

        KernelServices services = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        Assert.assertFalse(model.get(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME, CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED).isDefined());

        ModelNode read = createOperation(READ_ATTRIBUTE_OPERATION);
        read.get(NAME).set(CommonAttributes.SHOW_MODEL);
        Assert.assertFalse(services.executeForResult(read).asBoolean());

        ModelNode write = createOperation(WRITE_ATTRIBUTE_OPERATION);
        write.get(NAME).set(CommonAttributes.SHOW_MODEL);
        write.get(VALUE).set(true);
        services.executeForResult(write);

        model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME, CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED).isDefined());
        Assert.assertTrue(services.executeForResult(read).asBoolean());

        write.get(VALUE).set(false);
        services.executeForResult(write);

        model = services.readWholeModel();
        Assert.assertFalse(model.get(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME, CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED).isDefined());
        Assert.assertFalse(services.executeForResult(read).asBoolean());
    }

    @Test
    public void testTransformationAS712() throws Exception {
        testTransformation_1_0_0(ModelTestControllerVersion.V7_1_2_FINAL, "org.jboss.as:jboss-as-jmx:7.1.2.Final");
    }

    @Test
    public void testTransformationAS713() throws Exception {
        testTransformation_1_0_0(ModelTestControllerVersion.V7_1_3_FINAL, "org.jboss.as:jboss-as-jmx:7.1.3.Final");
    }

    private void testTransformation_1_0_0(ModelTestControllerVersion controllerVersion, String mavenGAV) throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "   <expose-resolved-model domain-name=\"jboss.RESOLVED\"/>" +
                "   <expose-expression-model domain-name=\"jboss.EXPRESSION\"/>" +
                "   <remoting-connector />" +
                "</subsystem>";

        ModelVersion oldVersion = ModelVersion.create(1, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(new BaseAdditionalInitalization())
                .setSubsystemXml(subsystemXml);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, oldVersion)
                .setExtensionClassName(JMXExtension.class.getName())
                .addMavenResourceURL(mavenGAV);
        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(oldVersion);
        Assert.assertNotNull(legacyServices);


        ModelNode legacyModel = checkSubsystemModelTransformation(mainServices, oldVersion);
        check_1_0_0_Model(legacyModel.get(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME), true, true);

        //Test that show-model=>expression is ignored
        ModelNode op = createOperation(WRITE_ATTRIBUTE_OPERATION, CommonAttributes.EXPOSE_MODEL, CommonAttributes.EXPRESSION);
        op.get(NAME).set(CommonAttributes.DOMAIN_NAME);
        op.get(VALUE).set("discarded");
        TransformedOperation transformedOp = mainServices.transformOperation(oldVersion, op);
        Assert.assertNull(transformedOp.getTransformedOperation());

        op = createOperation(READ_ATTRIBUTE_OPERATION, CommonAttributes.EXPOSE_MODEL, CommonAttributes.EXPRESSION);
        op.get(NAME).set(CommonAttributes.DOMAIN_NAME);
        transformedOp = mainServices.transformOperation(oldVersion, op);
        Assert.assertNull(transformedOp.getTransformedOperation());

        op = createOperation(ADD, CommonAttributes.EXPOSE_MODEL, CommonAttributes.EXPRESSION);
        transformedOp = mainServices.transformOperation(oldVersion, op);
        Assert.assertNull(transformedOp.getTransformedOperation());

        op = createOperation(REMOVE, CommonAttributes.EXPOSE_MODEL, CommonAttributes.EXPRESSION);
        transformedOp = mainServices.transformOperation(oldVersion, op);
        Assert.assertNull(transformedOp.getTransformedOperation());

        //Test the show-model=>resolved is converted
        op = createOperation(WRITE_ATTRIBUTE_OPERATION, CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED);
        op.get(NAME).set(CommonAttributes.DOMAIN_NAME);
        op.get(VALUE).set("discarded");
        final TransformedOperation operation = mainServices.transformOperation(oldVersion, op);
        Assert.assertNotNull(operation);
        Assert.assertNull(operation.getTransformedOperation());

        op = createOperation(READ_ATTRIBUTE_OPERATION, CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED);
        op.get(NAME).set(CommonAttributes.DOMAIN_NAME);
        transformedOp = mainServices.transformOperation(oldVersion, op);
        Assert.assertNull(transformedOp.getTransformedOperation());
        Assert.assertEquals(CommonAttributes.DEFAULT_RESOLVED_DOMAIN, mainServices.executeOperation(oldVersion, transformedOp).get(RESULT).asString());

        op = createOperation(REMOVE, CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED);
        transformedOp = mainServices.transformOperation(oldVersion, op);
        checkOutcome(mainServices.executeOperation(op));
        checkOutcome(mainServices.executeOperation(oldVersion, transformedOp));
        legacyModel = checkSubsystemModelTransformation(mainServices, oldVersion);
        check_1_0_0_Model(legacyModel.get(SUBSYSTEM, getMainSubsystemName()), true, false);


        op = createOperation(ADD, CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED);
        transformedOp = mainServices.transformOperation(oldVersion, op);
        checkOutcome(mainServices.executeOperation(op));
        checkOutcome(mainServices.executeOperation(oldVersion, transformedOp));
        legacyModel = checkSubsystemModelTransformation(mainServices, oldVersion);
        check_1_0_0_Model(legacyModel.get(SUBSYSTEM, getMainSubsystemName()), true, true);

    }

    @Test
    public void testRejectExpressionsAS712() throws Exception {
        testRejectExpressions_1_0_0(ModelTestControllerVersion.V7_1_2_FINAL, "org.jboss.as:jboss-as-jmx:7.1.2.Final");
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        testRejectExpressions_1_0_0(ModelTestControllerVersion.V7_1_3_FINAL, "org.jboss.as:jboss-as-jmx:7.1.3.Final");
    }

    /**
     * Tests rejection of expressions in 1.1.0 model.
     *
     * @throws Exception
     */
    private void testRejectExpressions_1_0_0(ModelTestControllerVersion controllerVersion, String mavenGAV) throws Exception {
        String subsystemXml =
            "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                    "   <remoting-connector use-management-endpoint=\"${test.exp:false}\"/>" +
                    "</subsystem>";

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        ModelVersion version_1_0_0 = ModelVersion.create(1, 0, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_0_0)
                .addMavenResourceURL(mavenGAV);

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_0_0);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME));
        ModelTestUtils.checkFailedTransformedBootOperations(
                mainServices,
                version_1_0_0,
                builder.parseXml(subsystemXml),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(
                                subsystemAddress.append(RemotingConnectorResource.REMOTE_CONNECTOR_CONFIG_PATH),
                                new FailedOperationTransformationConfig.RejectExpressionsConfig(RemotingConnectorResource.USE_MANAGEMENT_ENDPOINT))
                        );

    }

    private void check_1_0_0_Model(ModelNode legacySubsystem, boolean remotingConnector, boolean showModel) {
        Assert.assertEquals(2, legacySubsystem.keys().size());
        Assert.assertTrue(legacySubsystem.hasDefined(CommonAttributes.SHOW_MODEL));
        Assert.assertEquals(showModel, legacySubsystem.get(CommonAttributes.SHOW_MODEL).asBoolean());
        Assert.assertTrue(legacySubsystem.hasDefined(CommonAttributes.REMOTING_CONNECTOR));
        Assert.assertTrue(legacySubsystem.get(CommonAttributes.REMOTING_CONNECTOR).hasDefined(CommonAttributes.JMX));
    }

    private void assertJmxConnectorAddress(ModelNode address) {
        PathAddress addr = PathAddress.pathAddress(address);
        Assert.assertEquals(2, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(JMXExtension.SUBSYSTEM_NAME, element.getValue());
        element = addr.getElement(1);
        Assert.assertEquals(CommonAttributes.REMOTING_CONNECTOR, element.getKey());
        Assert.assertEquals(CommonAttributes.JMX, element.getValue());
    }

    private void assertJmxSubsystemAddress(ModelNode address) {
        PathAddress addr = PathAddress.pathAddress(address);
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(JMXExtension.SUBSYSTEM_NAME, element.getValue());
    }

    private static ModelNode createOperation(String name, String...addressElements) {
        final ModelNode addr = new ModelNode();
        addr.add(SUBSYSTEM, "jmx");
        for (int i = 0 ; i < addressElements.length ; i++) {
            addr.add(addressElements[i], addressElements[++i]);
        }
        return Util.getEmptyOperation(name, addr);
    }

    private static class BaseAdditionalInitalization extends AdditionalInitialization {

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                                        ManagementResourceRegistration rootRegistration) {
            rootRegistration.registerReadOnlyAttribute(LAUNCH_TYPE, new OperationStepHandler() {

                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.getResult().set(TYPE_STANDALONE);
                }
            }, AttributeAccess.Storage.RUNTIME);
        }

        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            controllerInitializer.addSocketBinding("remote", 12345);
            controllerInitializer.addPath("jboss.controller.temp.dir", System.getProperty("java.io.tmpdir"), null);
        }

        @Override
        protected void addExtraServices(final ServiceTarget target) {
            ManagementRemotingServices.installRemotingEndpoint(target, ManagementRemotingServices.MANAGEMENT_ENDPOINT, "loaclhost", EndpointService.EndpointType.MANAGEMENT, null, null);
            ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "jboss.controller.temp.dir");

            RemotingServices.installSecurityServices(target, "remote", null, null, tmpDirPath, null, null);
            RemotingServices.installConnectorServicesForSocketBinding(target, ManagementRemotingServices.MANAGEMENT_ENDPOINT, "remote", SocketBinding.JBOSS_BINDING_NAME.append("remote"), OptionMap.EMPTY, null, null);
        }
    }
}
