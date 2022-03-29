/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.transaction;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.io.FilePermission;
import java.io.IOException;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests for EAP7-981 / WFLY-10009
 *
 * @author istraka
 */
@RunWith(Arquillian.class)
@ServerSetup({MaximumTimeoutTestCase.TimeoutSetup.class})
public class MaximumTimeoutTestCase {

    private static final String MESSAGE_REGEX = ".*\\bWARN\\b.*WFLYTX0039:.*%d";

    private static final String MAX_TIMEOUT_ATTR = "maximum-timeout";
    private static final String DEF_TIMEOUT_ATTR = "default-timeout";

    private static final int MAX_TIMEOUT1 = 400;
    private static final int MAX_TIMEOUT2 = 500;

    private static final int DEFAULT_TIMEOUT = 100;
    private static final int NO_TIMEOUT = 0;

    private static final PathAddress TXN_SUBSYSTEM_ADDRESS = PathAddress.pathAddress().append(SUBSYSTEM, "transactions");
    private static final PathAddress LOG_FILE_ADDRESS = PathAddress.pathAddress()
            .append(SUBSYSTEM, "logging")
            .append("log-file", "server.log");
    private static final Logger LOGGER = Logger.getLogger(MaximumTimeoutTestCase.class);

    @Resource(mappedName = "java:/TransactionManager")
    private static TransactionManager txm;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(MaximumTimeoutTestCase.class, MaximumTimeoutTestCase.TimeoutSetup.class,
                        ManagementOperations.class, MgmtOperationException.class)
                .addPackage(TestXAResource.class.getPackage())
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller, org.jboss.remoting\n"), "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(
                        // ManagementClient needs the following permissions and a dependency on 'org.jboss.remoting3' module
                        new RemotingPermission("createEndpoint"),
                        new RemotingPermission("connect"),
                        new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
                ), "permissions.xml");
    }

    static void setMaximumTimeout(final ModelControllerClient client, int timeout) throws IOException, MgmtOperationException {
        ModelNode writeMaxTimeout = Util.getWriteAttributeOperation(TXN_SUBSYSTEM_ADDRESS, MAX_TIMEOUT_ATTR, timeout);
        ManagementOperations.executeOperation(client, writeMaxTimeout);
    }

    static void setDefaultTimeout(final ModelControllerClient client, int timeout) throws IOException, MgmtOperationException {
        ModelNode writeMaxTimeout = Util.getWriteAttributeOperation(TXN_SUBSYSTEM_ADDRESS, DEF_TIMEOUT_ATTR, timeout);
        ManagementOperations.executeOperation(client, writeMaxTimeout);
    }

    static List<ModelNode> getLogs(final ModelControllerClient client) throws IOException, MgmtOperationException {
        // /subsystem=logging/log-file=server.log:read-log-file(lines=-1)
        ModelNode op = Util.createEmptyOperation("read-log-file", LOG_FILE_ADDRESS);
        op.get("lines").set(-1);
        return ManagementOperations.executeOperation(client, op).asList();
    }



    @Test
    public void testMaximumTimeout() throws Exception {
        setDefaultTimeout(managementClient.getControllerClient(), NO_TIMEOUT);
        setMaximumTimeout(managementClient.getControllerClient(), MAX_TIMEOUT1);
        txm.setTransactionTimeout(NO_TIMEOUT);

        XAResource xaer = new TestXAResource();
        txm.begin();
        txm.getTransaction().enlistResource(xaer);
        int timeout = xaer.getTransactionTimeout();
        Assert.assertEquals(MAX_TIMEOUT1, timeout);
        txm.commit();

        setMaximumTimeout(managementClient.getControllerClient(), MAX_TIMEOUT2);
        xaer = new TestXAResource();
        txm.begin();
        txm.getTransaction().enlistResource(xaer);
        timeout = xaer.getTransactionTimeout();
        Assert.assertEquals(MAX_TIMEOUT2, timeout);
        txm.commit();
    }

    @Test
    public void testDefaultTimeout() throws Exception {
        setMaximumTimeout(managementClient.getControllerClient(), MAX_TIMEOUT1);
        setDefaultTimeout(managementClient.getControllerClient(), DEFAULT_TIMEOUT);

        XAResource xaer = new TestXAResource();
        txm.setTransactionTimeout(NO_TIMEOUT);
        txm.begin();
        txm.getTransaction().enlistResource(xaer);
        int timeout = xaer.getTransactionTimeout();
        Assert.assertEquals(DEFAULT_TIMEOUT, timeout);
        txm.commit();

        xaer = new TestXAResource();
        txm.setTransactionTimeout(20);
        txm.begin();
        txm.getTransaction().enlistResource(xaer);
        timeout = xaer.getTransactionTimeout();
        Assert.assertEquals(20, timeout);
        txm.commit();

        xaer = new TestXAResource();
        txm.setTransactionTimeout(NO_TIMEOUT);
        txm.begin();
        txm.getTransaction().enlistResource(xaer);
        timeout = xaer.getTransactionTimeout();
        Assert.assertEquals(DEFAULT_TIMEOUT, timeout);
        txm.commit();
    }

    @Test
    public void testLogFile() throws Exception {
        setMaximumTimeout(managementClient.getControllerClient(), MAX_TIMEOUT1);
        setDefaultTimeout(managementClient.getControllerClient(), NO_TIMEOUT);
        setMaximumTimeout(managementClient.getControllerClient(), MAX_TIMEOUT2);

        List<ModelNode> nodes = getLogs(managementClient.getControllerClient());
        boolean firstMessageFound = false;
        boolean secondMessageFound = false;
        for (ModelNode node : nodes) {
            String line = node.asString();
            if (!firstMessageFound) {
                if (line.matches(String.format(MESSAGE_REGEX, MAX_TIMEOUT1))) {
                    firstMessageFound = true;
                }
            } else {
                if (line.matches(String.format(MESSAGE_REGEX, MAX_TIMEOUT2))) {
                    secondMessageFound = true;
                }
            }
        }

        Assert.assertTrue(firstMessageFound && secondMessageFound);
    }

    static class TimeoutSetup implements ServerSetupTask {

        private int defaultTimeout;
        private int maxTimeout;

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode op = ManagementOperations.executeOperation(managementClient.getControllerClient(), Util.getReadAttributeOperation(TXN_SUBSYSTEM_ADDRESS, MAX_TIMEOUT_ATTR));
            maxTimeout = op.asInt();

            op = ManagementOperations.executeOperation(managementClient.getControllerClient(), Util.getReadAttributeOperation(TXN_SUBSYSTEM_ADDRESS, DEF_TIMEOUT_ATTR));
            defaultTimeout = op.asInt();
            LOGGER.debug("max timeout: " + maxTimeout);
            LOGGER.debug("default timeout: " + defaultTimeout);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            setDefaultTimeout(managementClient.getControllerClient(), defaultTimeout);
            setMaximumTimeout(managementClient.getControllerClient(), maxTimeout);
            setup(managementClient, s);
        }
    }
}