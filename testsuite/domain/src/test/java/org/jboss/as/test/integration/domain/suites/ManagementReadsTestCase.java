/*
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

package org.jboss.as.test.integration.domain.suites;

import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateResponse;

/**
 * Test of various read operations against the domain controller.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagementReadsTestCase {

    private static final String PATH_SEPARATOR = System.getProperty("file.separator");

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ManagementReadsTestCase.class.getSimpleName());

        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        DomainTestSuite.stopSupport();
        testSupport = null;
        domainMasterLifecycleUtil = null;
        domainSlaveLifecycleUtil = null;
    }

    @Test
    public void testDomainReadResource() throws IOException {
        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        final ModelNode domainOp = new ModelNode();
        domainOp.get(OP).set(READ_RESOURCE_OPERATION);
        domainOp.get(OP_ADDR).setEmptyList();
        domainOp.get(RECURSIVE).set(true);
        domainOp.get("proxies").set(false);

        ModelNode response = domainClient.execute(domainOp);
        validateResponse(response);

        // TODO make some more assertions about result content
    }

    @Test
    public void testHostReadResource() throws IOException {
        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        final ModelNode hostOp = new ModelNode();
        hostOp.get(OP).set(READ_RESOURCE_OPERATION);
        hostOp.get(OP_ADDR).setEmptyList().add(HOST, "master");
        hostOp.get(RECURSIVE).set(true);
        hostOp.get("proxies").set(false);

        ModelNode response = domainClient.execute(hostOp);
        validateResponse(response);
        // TODO make some more assertions about result content

        hostOp.get(OP_ADDR).setEmptyList().add(HOST, "slave");
        response = domainClient.execute(hostOp);
        validateResponse(response);
        // TODO make some more assertions about result content
    }

    @Test
    public void testHostReadResourceViaSlave() throws IOException {
        DomainClient domainClient = domainSlaveLifecycleUtil.getDomainClient();
        final ModelNode hostOp = new ModelNode();
        hostOp.get(OP).set(READ_RESOURCE_OPERATION);
        hostOp.get(OP_ADDR).setEmptyList().add(HOST, "slave");
        hostOp.get(RECURSIVE).set(true);
        hostOp.get("proxies").set(false);

        ModelNode response = domainClient.execute(hostOp);
        validateResponse(response);
        // TODO make some more assertions about result content
    }

    @Test
    public void testServerReadResource() throws IOException {
        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        final ModelNode serverOp = new ModelNode();
        serverOp.get(OP).set(READ_RESOURCE_OPERATION);
        ModelNode address = serverOp.get(OP_ADDR);
        address.add(HOST, "master");
        address.add(SERVER, "main-one");
        serverOp.get(RECURSIVE).set(true);
        serverOp.get("proxies").set(false);

        ModelNode response = domainClient.execute(serverOp);
        validateResponse(response);
        // TODO make some more assertions about result content

        address.setEmptyList();
        address.add(HOST, "slave");
        address.add(SERVER, "main-three");
        response = domainClient.execute(serverOp);
        validateResponse(response);
        // TODO make some more assertions about result content
    }

    @Test
    public void testServerReadResourceViaSlave() throws IOException {
        DomainClient domainClient = domainSlaveLifecycleUtil.getDomainClient();
        final ModelNode serverOp = new ModelNode();
        serverOp.get(OP).set(READ_RESOURCE_OPERATION);
        ModelNode address = serverOp.get(OP_ADDR);
        address.add(HOST, "slave");
        address.add(SERVER, "main-three");
        serverOp.get(RECURSIVE).set(true);
        serverOp.get("proxies").set(false);

        ModelNode response = domainClient.execute(serverOp);
        validateResponse(response);
        // TODO make some more assertions about result content
    }

    @Test
    public void testServerPathOverride() throws IOException {
        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode address = new ModelNode();
        address.add(HOST, "master");
        address.add(SERVER, "main-one");
        address.add(PATH, "domainTestPath");

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);

        final ModelNode response = client.execute(operation);
        validateResponse(response);

        final ModelNode result = response.get(RESULT);
        Assert.assertEquals("main-one", result.get(PATH).asString());
        Assert.assertEquals("jboss.domain.temp.dir", result.get(RELATIVE_TO).asString());
    }

    @Test
    public void testHostPathOverride() throws IOException {
        final DomainClient client = domainSlaveLifecycleUtil.getDomainClient();

        final ModelNode address = new ModelNode();
        address.add(HOST, "slave");
        address.add(SERVER, "main-three");
        address.add(PATH, "domainTestPath");

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);

        final ModelNode response = client.execute(operation);
        validateResponse(response);

        final ModelNode result = response.get(RESULT);
        Assert.assertEquals("/tmp", result.get(PATH).asString());
        Assert.assertFalse(result.get(RELATIVE_TO).isDefined());
    }

    @Test
    @Ignore("AS7-376")
    public void testReadResourceWildcards() throws IOException {
        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        final ModelNode request = new ModelNode();
        request.get(OP).set(READ_RESOURCE_OPERATION);
        ModelNode address = request.get(OP_ADDR);
        address.add(HOST, "*");
        address.add("running-server", "*");
        address.add(SUBSYSTEM, "*");
        request.get(RECURSIVE).set(true);
        request.get("proxies").set(false);

        ModelNode response = domainClient.execute(request);
        validateResponse(response);
        // TODO make some more assertions about result content

    }

    @Test
    public void testCompositeOperation() throws IOException {
        final DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        final ModelNode request = new ModelNode();
        request.get(OP).set(READ_RESOURCE_OPERATION);
        request.get(OP_ADDR).add("profile", "*");

        final ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();
        composite.get(STEPS).add(request);

        ModelNode response = domainClient.execute(composite);
        validateResponse(response);
        System.out.println(response);
    }

    @Test
    public void testDomainReadResourceDescription() throws IOException {

        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode request = new ModelNode();
        request.get(OP).set("read-resource-description");
        request.get(OP_ADDR).setEmptyList();
        request.get(RECURSIVE).set(true);
        request.get(OPERATIONS).set(true);

        ModelNode response = domainClient.execute(request);
        validateResponse(response);
        // TODO make some more assertions about result content
    }

    @Test
    public void testHostReadResourceDescription() throws IOException {

        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode request = new ModelNode();
        request.get(OP).set("read-resource-description");
        request.get(OP_ADDR).setEmptyList().add(HOST, "master");
        request.get(RECURSIVE).set(true);
        request.get(OPERATIONS).set(true);

        ModelNode response = domainClient.execute(request);
        validateResponse(response);
        // TODO make some more assertions about result content

        request.get(OP_ADDR).setEmptyList().add(HOST, "slave");
        response = domainClient.execute(request);
        validateResponse(response);
    }

    @Test
    public void testServerReadResourceDescription() throws IOException {

        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode request = new ModelNode();
        request.get(OP).set("read-resource-description");
        ModelNode address = request.get(OP_ADDR);
        address.add(HOST, "master");
        address.add(SERVER, "main-one");
        request.get(RECURSIVE).set(true);
        request.get(OPERATIONS).set(true);

        ModelNode response = domainClient.execute(request);
        validateResponse(response);
        // TODO make getDeploymentManager();some more assertions about result content

        address.setEmptyList();
        address.add(HOST, "slave");
        address.add(SERVER, "main-three");
        response = domainClient.execute(request);
        validateResponse(response);
        // TODO make some more assertions about result content
    }

    @Test
    public void testDomainReadConfigAsXml() throws IOException {

        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode request = new ModelNode();
        request.get(OP).set("read-config-as-xml");
        request.get(OP_ADDR).setEmptyList();

        ModelNode response = domainClient.execute(request);
        validateResponse(response);
        // TODO make some more assertions about result content
    }

    @Test
    public void testHostReadConfigAsXml() throws IOException {

        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode request = new ModelNode();
        request.get(OP).set("read-config-as-xml");
        request.get(OP_ADDR).setEmptyList().add(HOST, "master");

        ModelNode response = domainClient.execute(request);
        validateResponse(response);
        // TODO make some more assertions about result content

        request.get(OP_ADDR).setEmptyList().add(HOST, "slave");
        response = domainClient.execute(request);
        validateResponse(response);
    }

    @Test
    public void testServerReadConfigAsXml() throws IOException {

        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode request = new ModelNode();
        request.get(OP).set("read-config-as-xml");
        ModelNode address = request.get(OP_ADDR);
        address.add(HOST, "master");
        address.add(SERVER, "main-one");

        ModelNode response = domainClient.execute(request);
        validateResponse(response);
        // TODO make some more assertions about result content

        address.setEmptyList();
        address.add(HOST, "slave");
        address.add(SERVER, "main-three");
        response = domainClient.execute(request);
        validateResponse(response);
        // TODO make some more assertions about result content
    }

    @Test
    public void testResolveExpressionOnDomain() throws Exception  {
        ModelNode op = testSupport.createOperationNode(null, "resolve-expression-on-domain");
        op.get("expression").set("${file.separator}");

        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode response = domainClient.execute(op);
        ModelNode result = validateResponse(response);
        validateResolveExpressionOnMaster(result);
        validateResolveExpressionOnSlave(result);
    }

    @Test
    public void testResolveExpressionOnMasterHost() throws Exception  {
        ModelNode op = testSupport.createOperationNode("host=master", "resolve-expression-on-domain");
        op.get("expression").set("${file.separator}");

        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode response = domainClient.execute(op);
        ModelNode result = validateResponse(response);
        validateResolveExpressionOnMaster(result);
    }

    @Test
    public void testResolveExpressionOnSlaveHost() throws Exception  {
        resolveExpressionOnSlaveHostTest(domainMasterLifecycleUtil.getDomainClient());
    }

    @Test
    public void testResolveExpressionOnSlaveHostDirect() throws Exception  {
        resolveExpressionOnSlaveHostTest(domainSlaveLifecycleUtil.getDomainClient());
    }

    private void resolveExpressionOnSlaveHostTest(ModelControllerClient domainClient) throws Exception {
        ModelNode op = testSupport.createOperationNode("host=slave", "resolve-expression-on-domain");
        op.get("expression").set("${file.separator}");

        ModelNode response = domainClient.execute(op);
        ModelNode result = validateResponse(response);
        validateResolveExpressionOnSlave(result);
    }

    private static void validateResolveExpressionOnMaster(final ModelNode result) {
        System.out.println(result);
        ModelNode serverResult = result.get("server-groups", "main-server-group", "main-one");
        Assert.assertTrue(serverResult.isDefined());
        Assert.assertEquals("master", serverResult.get("host").asString());
        validateResolveExpressionOnServer(serverResult);
    }

    private static void validateResolveExpressionOnSlave(final ModelNode result) {
        System.out.println(result);
        ModelNode serverResult = result.get("server-groups", "main-server-group", "main-three");
        Assert.assertTrue(serverResult.isDefined());
        Assert.assertEquals("slave", serverResult.get("host").asString());
        validateResolveExpressionOnServer(serverResult);

        serverResult = result.get("server-groups", "other-server-group", "other-two");
        Assert.assertTrue(serverResult.isDefined());
        Assert.assertEquals("slave", serverResult.get("host").asString());
        validateResolveExpressionOnServer(serverResult);
    }

    private static void validateResolveExpressionOnServer(final ModelNode result) {
        ModelNode serverResult = validateResponse(result.get("response"));
        Assert.assertEquals(PATH_SEPARATOR, serverResult.asString());
    }
}
