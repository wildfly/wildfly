/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.session;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

import io.undertow.util.StatusCodes;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SessionStatisticsTestCase.SessionStatisticsServerSetup.class)
public class SessionStatisticsTestCase {
    private static final String ACTIVE_SESSIONS = "active-sessions";
    private static final String EXPIRED_SESSIONS = "expired-sessions";
    private static final String HIGHEST_SESSION_COUNT = "highest-session-count";
    private static final String MAX_ACTIVE_SESSIONS = "max-active-sessions";
    private static final String REJECTED_SESSIONS = "rejected-sessions";
    private static final String SESSIONS_CREATED = "sessions-created";

    private static final int CLIENT_COUNT = 7;
    private static final int MAX_SESSIONS = 4;
    @ArquillianResource
    public ManagementClient managementClient;

    static class SessionStatisticsServerSetup extends SnapshotRestoreSetupTask {

        @Override
        public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode op = new ModelNode();
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(SUBSYSTEM, "undertow");
            op.get(NAME).set("statistics-enabled");
            op.get(VALUE).set(true);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    @ArquillianResource
    private URI uri;

    @Deployment
    public static Archive<?> dependent() {
        return ShrinkWrap.create(WebArchive.class, "stats.war")
                .addClasses(SessionTestServlet.class)
                .addAsWebInfResource(new StringAsset("<jboss-web><max-active-sessions>" + MAX_SESSIONS + "</max-active-sessions></jboss-web>"), "jboss-web.xml");
    }

    @Test
    public void testSessionManagementOperations() throws Exception {
        CloseableHttpClient[] clients = new CloseableHttpClient[CLIENT_COUNT];
        try {
            for (int i = 0; i < CLIENT_COUNT; ++i) {
                clients[i] = HttpClients.createDefault();
            }
            ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
            operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
            operation.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.parseCLIStyleAddress("/deployment=stats.war/subsystem=undertow").toModelNode());
            ModelNode opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            ModelNode result = opRes.get(ModelDescriptionConstants.RESULT);
            //check everything is zero
            Assert.assertEquals(0, result.get(ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(0, result.get(EXPIRED_SESSIONS).asInt());
            Assert.assertEquals(0, result.get(HIGHEST_SESSION_COUNT).asInt());
            Assert.assertEquals(4, result.get(MAX_ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(0, result.get(REJECTED_SESSIONS).asInt());
            Assert.assertEquals(0, result.get("session-avg-alive-time").asInt());
            Assert.assertEquals(0, result.get("session-max-alive-time").asInt());
            Assert.assertEquals(0, result.get(SESSIONS_CREATED).asInt());

            final HttpGet get = new HttpGet(uri.toString() + "/SessionPersistenceServlet");
            final HttpGet invalidate = new HttpGet(get.getURI().toString() + "?invalidate=true");
            HttpResponse res = clients[0].execute(get);
            Assert.assertEquals(StatusCodes.OK, res.getStatusLine().getStatusCode());
            EntityUtils.consume(res.getEntity());
            opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            result = opRes.get(ModelDescriptionConstants.RESULT);

            //create a session and check that it worked
            Assert.assertEquals(1, result.get(ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(0, result.get(EXPIRED_SESSIONS).asInt());
            Assert.assertEquals(1, result.get(HIGHEST_SESSION_COUNT).asInt());
            Assert.assertEquals(MAX_SESSIONS, result.get(MAX_ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(0, result.get(REJECTED_SESSIONS).asInt());
            Assert.assertEquals(1, result.get(SESSIONS_CREATED).asInt());

            //this should use the same session
            res = clients[0].execute(get);
            Assert.assertEquals(StatusCodes.OK, res.getStatusLine().getStatusCode());
            EntityUtils.consume(res.getEntity());
            opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            result = opRes.get(ModelDescriptionConstants.RESULT);

            Assert.assertEquals(1, result.get(ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(0, result.get(EXPIRED_SESSIONS).asInt());
            Assert.assertEquals(1, result.get(HIGHEST_SESSION_COUNT).asInt());
            Assert.assertEquals(MAX_SESSIONS, result.get(MAX_ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(0, result.get(REJECTED_SESSIONS).asInt());
            Assert.assertEquals(1, result.get(SESSIONS_CREATED).asInt());


            res = clients[0].execute(invalidate);
            Assert.assertEquals(StatusCodes.OK, res.getStatusLine().getStatusCode());
            EntityUtils.consume(res.getEntity());

            opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            result = opRes.get(ModelDescriptionConstants.RESULT);

            Assert.assertEquals(0, result.get(ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(1, result.get(EXPIRED_SESSIONS).asInt());
            Assert.assertEquals(1, result.get(HIGHEST_SESSION_COUNT).asInt());
            Assert.assertEquals(MAX_SESSIONS, result.get(MAX_ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(0, result.get(REJECTED_SESSIONS).asInt());
            Assert.assertEquals(1, result.get(SESSIONS_CREATED).asInt());

            for (int i = 0; i < CLIENT_COUNT; ++i) {
                res = clients[i].execute(get);
                Assert.assertEquals(i >= MAX_SESSIONS ? StatusCodes.INTERNAL_SERVER_ERROR : StatusCodes.OK, res.getStatusLine().getStatusCode());
                EntityUtils.consume(res.getEntity());
            }

            opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            result = opRes.get(ModelDescriptionConstants.RESULT);

            Assert.assertEquals(MAX_SESSIONS, result.get(ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(1, result.get(EXPIRED_SESSIONS).asInt());
            Assert.assertEquals(MAX_SESSIONS, result.get(HIGHEST_SESSION_COUNT).asInt());
            Assert.assertEquals(MAX_SESSIONS, result.get(MAX_ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(CLIENT_COUNT - MAX_SESSIONS, result.get(REJECTED_SESSIONS).asInt());
            Assert.assertEquals(MAX_SESSIONS + 1, result.get(SESSIONS_CREATED).asInt());

            for (int i = 0; i < MAX_SESSIONS; ++i) {
                res = clients[i].execute(invalidate);
                Assert.assertEquals(StatusCodes.OK, res.getStatusLine().getStatusCode());
                EntityUtils.consume(res.getEntity());
            }

            opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            result = opRes.get(ModelDescriptionConstants.RESULT);

            Assert.assertEquals(0, result.get(ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(MAX_SESSIONS + 1, result.get(EXPIRED_SESSIONS).asInt());
            Assert.assertEquals(MAX_SESSIONS, result.get(HIGHEST_SESSION_COUNT).asInt());
            Assert.assertEquals(MAX_SESSIONS, result.get(MAX_ACTIVE_SESSIONS).asInt());
            Assert.assertEquals(CLIENT_COUNT - MAX_SESSIONS, result.get(REJECTED_SESSIONS).asInt());
            Assert.assertEquals(MAX_SESSIONS + 1, result.get(SESSIONS_CREATED).asInt());


        } finally {
            for (CloseableHttpClient i : clients) {
                IoUtils.safeClose(i);
            }
        }
    }
}
