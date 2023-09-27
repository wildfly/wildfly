/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.session;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SessionInvalidateTestCase {

    @ArquillianResource
    public ManagementClient managementClient;


    @Deployment
    public static Archive<?> dependent() {
        return ShrinkWrap.create(WebArchive.class, "invalidate.war")
                .addClasses(SessionTestServlet.class);
    }

    @Test
    public void testInvalidateSessions() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set("invalidate-session");
            operation.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.parseCLIStyleAddress("/deployment=invalidate.war/subsystem=undertow").toModelNode());
            operation.get("session-id").set("fake");
            ModelNode opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals("success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            Assert.assertEquals(false, opRes.get(ModelDescriptionConstants.RESULT).asBoolean());
            HttpGet get = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/invalidate/SessionPersistenceServlet");
            HttpResponse res = client.execute(get);
            String sessionId = null;
            for (Header cookie : res.getHeaders("Set-Cookie")) {
                if (cookie.getValue().startsWith("JSESSIONID=")) {
                    sessionId = cookie.getValue().split("=")[1].split("\\.")[0];
                    break;
                }
            }
            Assert.assertNotNull(sessionId);
            String result = EntityUtils.toString(res.getEntity());
            assertEquals("0", result);
            result = runGet(get, client);
            assertEquals("1", result);
            result = runGet(get, client);
            assertEquals("2", result);

            operation.get("session-id").set(sessionId);
            opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals("success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            Assert.assertEquals(true, opRes.get(ModelDescriptionConstants.RESULT).asBoolean());

            result = runGet(get, client);
            assertEquals("0", result);
            result = runGet(get, client);
            assertEquals("1", result);
        }
    }

    private String runGet(HttpGet get, HttpClient client) throws IOException {
        HttpResponse res = client.execute(get);
        return EntityUtils.toString(res.getEntity());
    }
}
