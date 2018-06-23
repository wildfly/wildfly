/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ee.suspend;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUSPEND_STATE;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketPermission;
import java.net.URL;
import java.io.FilePermission;
import java.util.PropertyPermission;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for suspend/resume functionality with EE concurrency
 */
@RunWith(Arquillian.class)
public class EEConcurrencySuspendTestCase {

    protected static Logger log = Logger.getLogger(EEConcurrencySuspendTestCase.class);


    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive deployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ee-suspend.war");
        war.addPackage(EEConcurrencySuspendTestCase.class.getPackage());
        war.addPackage(HttpRequest.class.getPackage());
        war.addClass(TestSuiteEnvironment.class);
        war.addAsResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller, org.jboss.remoting \n"), "META-INF/MANIFEST.MF");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new RuntimePermission("modifyThread"),
                new PropertyPermission("management.address", "read"),
                new PropertyPermission("node0", "read"),
                new PropertyPermission("jboss.http.port", "read"),
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read"),
                new SocketPermission(TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")),
                "permissions.xml");
        return war;
    }

    @Test
    public void testRequestInShutdown() throws Exception {

        final String address = "http://" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort() + "/ee-suspend/ShutdownServlet";
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        boolean suspended = false;
        try {
            Future<Object> result = executorService.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return HttpRequest.get(address, 60, TimeUnit.SECONDS);
                }
            });

            Thread.sleep(1000); //nasty, but we need to make sure the HTTP request has started

            Assert.assertEquals(ShutdownServlet.TEXT, result.get());

            ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("suspend");
            execute(managementClient.getControllerClient(), op);

            op = new ModelNode();
            op.get(OP).set(READ_ATTRIBUTE_OPERATION);
            op.get(NAME).set(SUSPEND_STATE);

            waitUntilSuspendStateResult(op, "SUSPENDING");

            ShutdownServlet.requestLatch.countDown();

            op = new ModelNode();
            op.get(OP).set(READ_ATTRIBUTE_OPERATION);
            op.get(NAME).set(SUSPEND_STATE);

            waitUntilSuspendStateResult(op, "SUSPENDED");

            //server is now suspended,check we get 503 http status code
            final HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
            try {
                conn.setDoInput(true);
                int responseCode = conn.getResponseCode();
                Assert.assertEquals(503, responseCode);
            } finally {
                conn.disconnect();
            }

            suspended = true;
        } finally {
            ShutdownServlet.requestLatch.countDown();
            executorService.shutdown();

            if (suspended){
                //if suspended, test if it is resumed
                ModelNode op = new ModelNode();
                op.get(ModelDescriptionConstants.OP).set("resume");
                execute(managementClient.getControllerClient(), op);

                op = new ModelNode();
                op.get(OP).set(READ_ATTRIBUTE_OPERATION);
                op.get(NAME).set(SUSPEND_STATE);

                Assert.assertEquals("server-state is not <RUNNING> after resume operation. ", "RUNNING", executeForStringResult(managementClient.getControllerClient(), op));
            }
        }
    }

    private void waitUntilSuspendStateResult(ModelNode op, String expectedResult) throws IOException, InterruptedException {
        final long deadline = System.currentTimeMillis() + 4000;
        while (true) {
            String result = executeForStringResult(managementClient.getControllerClient(), op);
            if (result.equals(expectedResult)) {
                break;
            }
            if (System.currentTimeMillis() > deadline) {
                Assert.fail("Server suspend-state is not in " + expectedResult + " after " + deadline + " milliseconds.");
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }

    static ModelNode execute(final ModelControllerClient client, final ModelNode op) throws IOException {
        ModelNode result = client.execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            Assert.fail(Operations.getFailureDescription(result).asString());
        }
        return result;
    }

    static String executeForStringResult(final ModelControllerClient client, final ModelNode op) throws IOException {
        return execute(client,op).get(RESULT).asString();
    }
}
