/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForResult;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.startServer;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.waitUntilState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.jboss.as.controller.client.Notification;
import org.jboss.as.controller.client.NotificationFilter;
import org.jboss.as.controller.client.NotificationHandler;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test that notifications are emitted during server lifecycle
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc
 */
public class ServerNotificationTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;

    private static final ModelNode serverConfigAddress = new ModelNode();

    static {
        // (host=slave),(server-config=main-one)
        serverConfigAddress.add("host", "master");
        serverConfigAddress.add("server-config", "main-one");
    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ServerNotificationTestCase.class.getSimpleName());

        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        DomainTestSuite.stopSupport();
        testSupport = null;
        domainMasterLifecycleUtil = null;
    }

    @After
    public void stopServers() throws Exception {
        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode stopServer = new ModelNode();
        stopServer.get(OP).set("stop");
        stopServer.get(OP_ADDR).set(serverConfigAddress);
        client.execute(stopServer);
        waitUntilState(client, serverConfigAddress, "STOPPED");
    }

    @Test
    public void testServerStartedNotification() throws Exception {
        NotificationHandlerWithLatch handler = new NotificationHandlerWithLatch(1);

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        client.registerNotificationHandler(serverConfigAddress,
                handler,
                new NotificationFilter() {
                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return notification.getType().equals("server-started");
                    }
                });

        // Start main-one
        startServer(client, "master", "main-one");

        assertTrue("did not receive server-started notification", handler.getLatch().await(1, SECONDS));
    }

    @Test
    public void testServerStoppedNotification() throws Exception {
        NotificationHandlerWithLatch handler = new NotificationHandlerWithLatch(1);

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        // Start main-one
        startServer(client, "master", "main-one");

        client.registerNotificationHandler(serverConfigAddress,
                handler,
                new NotificationFilter() {
                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return notification.getType().equals("server-stopped");
                    }
                });

        final ModelNode stopServer = new ModelNode();
        stopServer.get(OP).set("stop");
        stopServer.get(OP_ADDR).set(serverConfigAddress);
        executeForResult(stopServer, client);

        assertTrue("did not receive server-stopped notification", handler.getLatch().await(1, SECONDS));
    }

    @Test
    public void testServerRestartedNotification() throws Exception {
        NotificationHandlerWithLatch handler = new NotificationHandlerWithLatch(1);

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        // Start main-one
        String state = startServer(client, "master", "main-one");
        assertEquals("STARTED", state);

        client.registerNotificationHandler(serverConfigAddress,
                handler,
                new NotificationFilter() {
                    @Override
                    public boolean isNotificationEnabled(Notification notification) {
                        return notification.getType().equals("server-restarted");
                    }
                });

        final ModelNode restartServer = new ModelNode();
        restartServer.get(OP_ADDR).set(serverConfigAddress);
        restartServer.get(OP).set("restart");
        executeForResult(restartServer, client);

        assertTrue("did not receive server-restarted notification", handler.getLatch().await(1, SECONDS));
    }

    private static class NotificationHandlerWithLatch implements NotificationHandler {

        private final CountDownLatch latch;

        private NotificationHandlerWithLatch(int num) {
            this.latch = new CountDownLatch(num);
        }

        @Override
        public void handleNotification(Notification notification) {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }
}
