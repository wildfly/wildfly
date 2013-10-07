/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.remoting.SaslPolicyResource.FORWARD_SECRECY;
import static org.jboss.as.remoting.SaslPolicyResource.NO_ACTIVE;
import static org.jboss.as.remoting.SaslPolicyResource.NO_ANONYMOUS;
import static org.jboss.as.remoting.SaslPolicyResource.NO_DICTIONARY;
import static org.jboss.as.remoting.SaslPolicyResource.NO_PLAIN_TEXT;
import static org.jboss.as.remoting.SaslPolicyResource.PASS_CREDENTIALS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat, inc
 */
public class RemotingSubsystemTransformersTestCase extends AbstractSubsystemBaseTest {

    public RemotingSubsystemTransformersTestCase() {
        super(RemotingExtension.SUBSYSTEM_NAME, new RemotingExtension());
    }

    @Test
    public void testExpressionsAreRejectedAS712() throws Exception {
        testExpressionsAreRejectedByVersion_1_1(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testExpressionsAreRejectedAS713() throws Exception {
        testExpressionsAreRejectedByVersion_1_1(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testExpressionsAreRejectedByVersion_1_1(ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = readResource("remoting-with-expressions.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // Add legacy subsystems
        ModelVersion version_1_1 = ModelVersion.create(1, 1);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, version_1_1)
                .addMavenResourceURL("org.jboss.as:jboss-as-remoting:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        PathAddress rootAddr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME));
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1, parse(subsystemXml),
                new FailedOperationTransformationConfig()
                    .addFailedAttribute(rootAddr,
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                    RemotingSubsystemRootResource.WORKER_READ_THREADS,
                                    RemotingSubsystemRootResource.WORKER_TASK_CORE_THREADS,
                                    RemotingSubsystemRootResource.WORKER_TASK_KEEPALIVE,
                                    RemotingSubsystemRootResource.WORKER_TASK_LIMIT,
                                    RemotingSubsystemRootResource.WORKER_TASK_MAX_THREADS,
                                    RemotingSubsystemRootResource.WORKER_WRITE_THREADS))
                    .addFailedAttribute(rootAddr.append(PathElement.pathElement(CommonAttributes.CONNECTOR)).append(PathElement.pathElement(CommonAttributes.PROPERTY)),
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(VALUE))
                    .addFailedAttribute(rootAddr.append(PathElement.pathElement(CommonAttributes.CONNECTOR)).append(PathElement.pathElement(CommonAttributes.SECURITY, CommonAttributes.SASL)),
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.SERVER_AUTH, CommonAttributes.REUSE_SESSION))
                    .addFailedAttribute(rootAddr.append(PathElement.pathElement(CommonAttributes.CONNECTOR))
                                .append(PathElement.pathElement(CommonAttributes.SECURITY, CommonAttributes.SASL)).append(PathElement.pathElement(CommonAttributes.SASL_POLICY)),
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                    CommonAttributes.FORWARD_SECRECY,
                                    CommonAttributes.NO_ACTIVE,
                                    CommonAttributes.NO_ANONYMOUS,
                                    CommonAttributes.NO_DICTIONARY,
                                    CommonAttributes.NO_PLAIN_TEXT,
                                    CommonAttributes.PASS_CREDENTIALS))
                    .addFailedAttribute(rootAddr.append(PathElement.pathElement(CommonAttributes.CONNECTOR))
                                .append(PathElement.pathElement(CommonAttributes.PROPERTY)),
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                    CommonAttributes.VALUE))
                    .addFailedAttribute(rootAddr.append(
                            PathElement.pathElement(CommonAttributes.CONNECTOR))
                                .append(PathElement.pathElement(CommonAttributes.SECURITY, CommonAttributes.SASL))
                                .append(CommonAttributes.PROPERTY),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                CommonAttributes.VALUE))
                    .addFailedAttribute(rootAddr.append(
                            PathElement.pathElement(CommonAttributes.OUTBOUND_CONNECTION))
                                .append(CommonAttributes.PROPERTY),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                CommonAttributes.VALUE))
                    .addFailedAttribute(rootAddr.append(PathElement.pathElement(CommonAttributes.REMOTE_OUTBOUND_CONNECTION)),
                            new FailedOperationTransformationConfig.ChainedConfig(
                                    toList(new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.USERNAME), new FixProtocolConfig(CommonAttributes.PROTOCOL)),
                                    CommonAttributes.USERNAME, CommonAttributes.PROTOCOL))
                    .addFailedAttribute(rootAddr.append(
                            PathElement.pathElement(CommonAttributes.REMOTE_OUTBOUND_CONNECTION))
                                .append(PathElement.pathElement(CommonAttributes.PROPERTY)),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.VALUE))
                    .addFailedAttribute(rootAddr.append(
                            PathElement.pathElement(CommonAttributes.LOCAL_OUTBOUND_CONNECTION))
                                .append(PathElement.pathElement(CommonAttributes.PROPERTY)),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.VALUE)));
    }

    @Test
    public void testNonRemotingProtocolRejectedAS720() throws Exception {
        testNonRemotingProtocolRejectedByVersion1_2(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    private void testNonRemotingProtocolRejectedByVersion1_2(ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = readResource("remoting-with-expressions.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // Add legacy subsystems
        ModelVersion version_1_1 = ModelVersion.create(1, 2);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, version_1_1)
                .addMavenResourceURL("org.jboss.as:jboss-as-remoting:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        PathAddress rootAddr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME));
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1, parse(subsystemXml),
                new FailedOperationTransformationConfig()
                    .addFailedAttribute(rootAddr.append(PathElement.pathElement(CommonAttributes.REMOTE_OUTBOUND_CONNECTION)),
                            new FixProtocolConfig(CommonAttributes.PROTOCOL)));
    }

    @Test
    public void testTransformersAS712() throws Exception {
        testTransformers_1_1(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformersAS713() throws Exception {
        testTransformers_1_1(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformers_1_1(ModelTestControllerVersion controllerVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("remoting-without-expressions.xml");
        ModelVersion oldVersion = ModelVersion.create(1, 1);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, oldVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-remoting:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(oldVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, oldVersion);
        checkRejectWorkerThreadAttributes(mainServices, oldVersion);
        checkRejectSASLAttribute(mainServices, oldVersion, CommonAttributes.REUSE_SESSION, "${reuse.session:true}");
        checkRejectSASLAttribute(mainServices, oldVersion, CommonAttributes.SERVER_AUTH, "${server.auth:true}");
        checkRejectSASLProperty(mainServices, oldVersion);
        checkRejectSASLPolicyAttributes(mainServices, oldVersion);
        checkRejectConnectorProperty(mainServices, oldVersion);
        checkRejectRemoteOutboundConnectionUsername(mainServices, oldVersion);
        checkRejectOutboundConnectionProperty(mainServices, oldVersion, CommonAttributes.REMOTE_OUTBOUND_CONNECTION, "remote-conn1");
        checkRejectOutboundConnectionProperty(mainServices, oldVersion, CommonAttributes.LOCAL_OUTBOUND_CONNECTION, "local-conn1");
        checkRejectOutboundConnectionProperty(mainServices, oldVersion, CommonAttributes.OUTBOUND_CONNECTION, "generic-conn1");
        checkRejectOutboundConnectionProtocolNotRemote(mainServices, oldVersion, CommonAttributes.REMOTE_OUTBOUND_CONNECTION, "remote-conn1");
        checkRejectHttpConnector(mainServices, oldVersion);
    }

    @Test
    public void testTransformersAS720() throws Exception {
        testTransformers_1_2(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    private void testTransformers_1_2(ModelTestControllerVersion controllerVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("remoting-with-expressions-and-good-legacy-protocol.xml");
        ModelVersion oldVersion = ModelVersion.create(1, 2, 0);


        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, oldVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-remoting:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(createAdditionalInitialization(), null);
        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(oldVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, oldVersion);
        checkRejectOutboundConnectionProtocolNotRemote(mainServices, oldVersion, CommonAttributes.REMOTE_OUTBOUND_CONNECTION, "remote-conn1");
        checkRejectHttpConnector(mainServices, oldVersion);
    }

    private void checkRejectOutboundConnectionProperty(KernelServices mainServices, ModelVersion version, String type, String name) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        address.add(type, name);
        address.add(CommonAttributes.PROPERTY, "org.xnio.Options.SSL_ENABLED");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(CommonAttributes.VALUE);
        operation.get(VALUE).set("${myprop:true}");

        checkReject(operation, mainServices, version);
    }

    private void checkRejectOutboundConnectionProtocolNotRemote(KernelServices mainServices, ModelVersion version, String type, String name) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        address.add(type, name);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(CommonAttributes.PROTOCOL);
        operation.get(VALUE).set(Protocol.HTTP_REMOTING.toString());

        checkReject(operation, mainServices, version);

        PathAddress addr = PathAddress.pathAddress(operation.get(OP_ADDR));
        PathElement element = addr.getLastElement();
        addr = addr.subAddress(0, addr.size() - 1);
        addr = addr.append(PathElement.pathElement(element.getKey(), "remoting-outbound2"));

        operation = Util.createAddOperation(addr);
        operation.get(CommonAttributes.OUTBOUND_SOCKET_BINDING_REF).set("dummy-outbound-socket");
        operation.get(CommonAttributes.USERNAME).set("myuser");
        operation.get(CommonAttributes.PROTOCOL).set(Protocol.HTTP_REMOTING.toString());
        checkReject(operation, mainServices, version);

    }

    private void checkRejectSASLAttribute(KernelServices mainServices, ModelVersion version, String name, String value) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        address.add(CommonAttributes.CONNECTOR, "remoting-connector");
        address.add(CommonAttributes.SECURITY, CommonAttributes.SASL);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(name);
        operation.get(VALUE).set(value);

        checkReject(operation, mainServices, version);
    }

    private void checkRejectSASLProperty(KernelServices mainServices, ModelVersion version) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        address.add(CommonAttributes.CONNECTOR, "remoting-connector");
        address.add(CommonAttributes.SECURITY, CommonAttributes.SASL);
        address.add(CommonAttributes.PROPERTY, "sasl1");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(CommonAttributes.VALUE);
        operation.get(VALUE).set("${sasl.prop:sasl one}");

        checkReject(operation, mainServices, version);
    }

    private void checkRejectSASLPolicyAttributes(KernelServices mainServices, ModelVersion version) throws OperationFailedException {
        for (AttributeDefinition attr: new AttributeDefinition[] {NO_ACTIVE, NO_ANONYMOUS, NO_DICTIONARY, FORWARD_SECRECY,
                NO_PLAIN_TEXT, PASS_CREDENTIALS}) {
            checkRejectSASLPolicyAttribute(mainServices, version, attr);
        }
    }

    private void checkRejectSASLPolicyAttribute(KernelServices mainServices, ModelVersion version, AttributeDefinition attr) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        address.add(CommonAttributes.CONNECTOR, "remoting-connector");
        address.add(CommonAttributes.SECURITY, CommonAttributes.SASL);
        address.add(CommonAttributes.SASL_POLICY, CommonAttributes.POLICY);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(attr.getName());
        operation.get(VALUE).set("${mypolicy:false}");

        checkReject(operation, mainServices, version);
    }

    private void checkRejectConnectorProperty(KernelServices mainServices, ModelVersion version) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        address.add(CommonAttributes.CONNECTOR, "remoting-connector");
        address.add(CommonAttributes.PROPERTY, "c1");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(CommonAttributes.VALUE);
        operation.get(VALUE).set("${connector.prop:connector one}");

        checkReject(operation, mainServices, version);
    }

    private void checkRejectRemoteOutboundConnectionUsername(KernelServices mainServices, ModelVersion version) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        address.add(CommonAttributes.REMOTE_OUTBOUND_CONNECTION, "remote-conn1");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(CommonAttributes.USERNAME);
        operation.get(VALUE).set("${remoting.user:myuser}");

        checkReject(operation, mainServices, version);
    }

    private void checkRejectWorkerThreadAttributes(KernelServices mainServices, ModelVersion version) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("worker-read-threads");
        operation.get(VALUE).set("${worker.read.threads:5}");

        checkReject(operation, mainServices, version);
    }

    private void checkRejectHttpConnector(KernelServices mainServices, ModelVersion version) throws OperationFailedException {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(CommonAttributes.CONNECTOR_REF).set("test");
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        address.add(HttpConnectorResource.PATH.getKey(), "test");
        operation.get(OP_ADDR).set(address);

        checkReject(operation, mainServices, version);
    }

    private void checkReject(ModelNode operation, KernelServices mainServices, ModelVersion version) throws OperationFailedException {

        ModelNode mainResult = mainServices.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());

        ModelNode successResult = new ModelNode();
        successResult.get(OUTCOME).set(SUCCESS);
        successResult.protect();
        ModelNode failedResult = new ModelNode();
        failedResult.get(OUTCOME).set(FAILED);
        failedResult.protect();
        ModelNode ignoreResult = new ModelNode();
        ignoreResult.get(OUTCOME).set(IGNORED);
        ignoreResult.protect();

        final OperationTransformer.TransformedOperation op = mainServices.transformOperation(version, operation);
        final ModelNode result = mainServices.executeOperation(version, op);
        assertEquals("should reject the expression", FAILED, result.get(OUTCOME).asString());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("remoting-with-expressions.xml");
    }

    @Override
    protected String getSubsystemXml(String resource) throws IOException {
        return readResource(resource);
    }

    private List<AttributesPathAddressConfig<?>> toList(AttributesPathAddressConfig<?>...configs){
        List<AttributesPathAddressConfig<?>> list = new ArrayList<AttributesPathAddressConfig<?>>();
        for (AttributesPathAddressConfig<?> config : configs) {
            list.add(config);
        }
        return list;
    }

    private static class FixProtocolConfig extends AttributesPathAddressConfig<FixProtocolConfig> {

        public FixProtocolConfig(String... attributes) {
            super(attributes);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.asString().equals(Protocol.REMOTE.toString());
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(Protocol.REMOTE.toString());
        }

    }
}
