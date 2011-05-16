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

package org.jboss.as.test.integration.domain;

import org.jboss.as.arquillian.container.domain.managed.DomainLifecycleUtil;
import org.jboss.as.arquillian.container.domain.managed.JBossAsManagedConfiguration;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Test of various read operations against the domain controller.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagementReadsTestCase {

    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    @BeforeClass
    public static void setupDomain() throws Exception {

        final JBossAsManagedConfiguration masterConfig = DomainTestUtil.getMasterConfiguration("domain-configs/domain-standard.xml", "host-configs/host-master.xml", ManagementReadsTestCase.class.getSimpleName());
        domainMasterLifecycleUtil = new DomainLifecycleUtil(masterConfig);

        domainMasterLifecycleUtil.start();

        final JBossAsManagedConfiguration slaveConfig = DomainTestUtil.getSlaveConfiguration("host-configs/host-slave.xml", ManagementReadsTestCase.class.getSimpleName());
        domainSlaveLifecycleUtil = new DomainLifecycleUtil(slaveConfig);

        // TODO replace synchronous start with async calls
//        DomainTestUtil.startHosts(DomainTestUtil.domainBootTimeout, domainMasterLifecycleUtil, domainSlaveLifecycleUtil);

        domainSlaveLifecycleUtil.start();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {

        // TODO replace synchronous stop with async calls
//        if (domainMasterLifecycleUtil != null && domainSlaveLifecycleUtil != null) {
//            DomainTestUtil.stopHosts(DomainTestUtil.domainShutdownTimeout, domainMasterLifecycleUtil, domainSlaveLifecycleUtil);
//        }

        try {
            if (domainSlaveLifecycleUtil != null) {
                domainSlaveLifecycleUtil.stop();
            }
        }   finally {
            if (domainMasterLifecycleUtil != null) {
                domainMasterLifecycleUtil.stop();
            }
        }
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
        // TODO make some more assertions about result content

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



    private ModelNode validateResponse(ModelNode response) {

        if(! SUCCESS.equals(response.get(OUTCOME).asString())) {
            Assert.fail(response.get(FAILURE_DESCRIPTION).toString());
        }

        Assert.assertTrue("result is defined", response.hasDefined(RESULT));
        return response.get(RESULT);
    }
}
