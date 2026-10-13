/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.mgmt;

import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Session;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunAsClient()
@RunWith(Arquillian.class)
public class JMSServerManagementTestCase {
    private static final Logger LOGGER = Logger.getLogger(JMSServerManagementTestCase.class);
    @ContainerResource
    private Context remoteContext;
    @ContainerResource
    private ManagementClient managementClient;
    private JMSOperations adminSupport;

    @Before
    public void setup() throws Exception {
        adminSupport = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
    }

    @After
    public void teardown() throws Exception {
        if (adminSupport != null) {
            adminSupport.close();
        }
    }

    private static JsonArray fromString(String string) {
        try (JsonReader reader = Json.createReader(new StringReader(string))) {
            return reader.readArray();
        }
    }

    @Test
    public void testListAllSessions() throws Exception {
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        try (Connection conn = cf.createConnection("guest", "guest");
                Session session = conn.createSession();
                Connection test = cf.createConnection("guest", "guest");
                Session sessionTest = test.createSession()) {
            ModelNode result = execute(getJMSServerOperation("list-all-sessions-as-json"), true);
            assertTrue(result.isDefined());
            assertEquals(ModelType.STRING, result.getType());
            JsonArray sessions = fromString(result.asString());
            //Each JMS session creates 2 sessions on the broker: st session is used for authentication then the 2nd for messages.
            assertEquals(sessions.toString(), 4, sessions.size());
            for (JsonValue jsonValue : sessions) {
                JsonObject jsonSession = jsonValue.asJsonObject();
                assertNotNull(jsonSession.getString("sessionID"));
                assertNotNull(jsonSession.getJsonNumber("creationTime").longValue());
                assertEquals(0, jsonSession.getInt("consumerCount"));
                assertEquals("guest", jsonSession.getString("validatedUser"));
                assertEquals("guest", jsonSession.getString("principal"));
                if (jsonSession.containsKey("metadata")) {
                    assertTrue(jsonSession.get("metadata").asJsonObject().containsKey("jms-session"));
                }
            }
        }
    }

    @Test
    public void testListConnections() throws Exception {
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        try (Connection conn = cf.createConnection("guest", "guest");
            Session session = conn.createSession()) {
            ModelNode result = execute(getJMSServerOperation("list-connections-as-json"), true);
            assertTrue(result.isDefined());
            assertEquals(ModelType.STRING, result.getType());
            JsonArray sessions = fromString(result.asString());
            //Each JMS session creates 2 sessions on the broker: st session is used for authentication then the 2nd for messages.
            assertEquals(sessions.toString(), 2, sessions.size());
            for (JsonValue jsonValue : sessions) {
                JsonObject jsonConnection = jsonValue.asJsonObject();
                assertNotNull(jsonConnection.getString("connectionID"));
                assertNotNull(jsonConnection.getJsonNumber("creationTime").longValue());
                String clientAddress = jsonConnection.getString("clientAddress");
                assertEquals("RemotingConnectionImpl", jsonConnection.getString("implementation"));
                assertNotNull(clientAddress);
                if("invm:0".equals(clientAddress)) {
                    assertEquals(0, jsonConnection.getInt("sessionCount"));
                } else {
                    assertEquals(2, jsonConnection.getInt("sessionCount"));
                }
            }
        }
    }

    private ModelNode getJMSServerOperation(String operationName) {
        final ModelNode address = adminSupport.getServerAddress();
        return getEmptyOperation(operationName, address);
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            if (!"success".equals(outcome)) {
                LOGGER.trace(response);
            }
            Assert.assertEquals("success", outcome);
            return response.get("result");
        } else {
            if ("success".equals(outcome)) {
                LOGGER.trace(response);
            }
            Assert.assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }
}
