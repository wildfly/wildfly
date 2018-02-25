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

import static org.wildfly.extension.undertow.Constants.ATTRIBUTE;
import static org.wildfly.extension.undertow.Constants.GET_SESSION_ATTRIBUTE;
import static org.wildfly.extension.undertow.Constants.GET_SESSION_CREATION_TIME;
import static org.wildfly.extension.undertow.Constants.GET_SESSION_CREATION_TIME_MILLIS;
import static org.wildfly.extension.undertow.Constants.GET_SESSION_LAST_ACCESSED_TIME;
import static org.wildfly.extension.undertow.Constants.GET_SESSION_LAST_ACCESSED_TIME_MILLIS;
import static org.wildfly.extension.undertow.Constants.INVALIDATE_SESSION;
import static org.wildfly.extension.undertow.Constants.LIST_SESSIONS;
import static org.wildfly.extension.undertow.Constants.LIST_SESSION_ATTRIBUTES;
import static org.wildfly.extension.undertow.Constants.LIST_SESSION_ATTRIBUTE_NAMES;
import static org.wildfly.extension.undertow.Constants.SESSION_ID;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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
public class SessionManagementTestCase {

    @ArquillianResource
    public ManagementClient managementClient;


    @Deployment
    public static Archive<?> dependent() {
        return ShrinkWrap.create(WebArchive.class, "management.war")
                .addClasses(SessionTestServlet.class);
    }

    @Test
    public void testSessionManagementOperations() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set(LIST_SESSIONS);
            operation.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.parseCLIStyleAddress("/deployment=management.war/subsystem=undertow").toModelNode());
            ModelNode opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            Assert.assertEquals(Collections.emptyList(), opRes.get(ModelDescriptionConstants.RESULT).asList());
            long c1 = System.currentTimeMillis();
            HttpGet get = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/management/SessionPersistenceServlet");
            HttpResponse res = client.execute(get);
            long c2 = System.currentTimeMillis();
            String sessionId = null;
            for (Header cookie : res.getHeaders("Set-Cookie")) {
                if (cookie.getValue().startsWith("JSESSIONID=")) {
                    sessionId = cookie.getValue().split("=")[1].split("\\.")[0];
                    break;
                }
            }
            Assert.assertNotNull(sessionId);
            opRes = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
            Assert.assertEquals(opRes.toString(), Collections.singletonList(new ModelNode(sessionId)), opRes.get(ModelDescriptionConstants.RESULT).asList());

            operation.get(SESSION_ID).set(sessionId);

            opRes = executeOperation(operation, GET_SESSION_CREATION_TIME_MILLIS);
            long time1 = opRes.get(ModelDescriptionConstants.RESULT).asLong();
            Assert.assertTrue(c1 <= time1);
            Assert.assertTrue(time1 <= c2);

            opRes = executeOperation(operation, GET_SESSION_CREATION_TIME);
            long sessionCreationTime = LocalDateTime.parse(opRes.get(ModelDescriptionConstants.RESULT).asString(), DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now())).toEpochMilli();
            Assert.assertEquals(time1, sessionCreationTime);

            opRes = executeOperation(operation, GET_SESSION_LAST_ACCESSED_TIME_MILLIS);
            Assert.assertEquals(time1, opRes.get(ModelDescriptionConstants.RESULT).asLong());

            opRes = executeOperation(operation, GET_SESSION_LAST_ACCESSED_TIME);
            long aTime2 = LocalDateTime.parse(opRes.get(ModelDescriptionConstants.RESULT).asString(), DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now())).toEpochMilli();
            Assert.assertEquals(time1, aTime2);
            Assert.assertEquals(sessionCreationTime, aTime2);

            opRes = executeOperation(operation, LIST_SESSION_ATTRIBUTE_NAMES);
            List<ModelNode> resultList = opRes.get(ModelDescriptionConstants.RESULT).asList();
            Assert.assertEquals(1, resultList.size());
            Assert.assertEquals(opRes.toString(), "val", resultList.get(0).asString());

            opRes = executeOperation(operation, LIST_SESSION_ATTRIBUTES);
            List<Property> properties = opRes.get(ModelDescriptionConstants.RESULT).asPropertyList();
            Assert.assertEquals(opRes.toString(), 1, properties.size());
            Property property = properties.get(0);
            Assert.assertEquals(opRes.toString(), "val", property.getName());
            Assert.assertEquals(opRes.toString(), "0", property.getValue().asString());

            //we want to make sure that the values will be different
            //so we wait 10ms
            Thread.sleep(10);
            long a1 = System.currentTimeMillis();
            client.execute(get);
            long a2 = System.currentTimeMillis();

            do {
                //because the last access time is updated after the request returns there is a possible race here
                //to get around this we execute this op in a loop and wait for the value to change
                //in 99% of cases this will only iterate once
                //because of the 10ms sleep above they should ways be different
                //we have a max wait time of 1s if something goes wrong
                opRes = executeOperation(operation, GET_SESSION_LAST_ACCESSED_TIME_MILLIS);
                time1 = opRes.get(ModelDescriptionConstants.RESULT).asLong();
                if(time1 != sessionCreationTime) {
                    break;
                }
            } while (System.currentTimeMillis() < a1 + 1000);
            Assert.assertTrue(a1 <= time1);
            Assert.assertTrue(time1 <= a2);

            opRes = executeOperation(operation, GET_SESSION_LAST_ACCESSED_TIME);
            long time2 = LocalDateTime.parse(opRes.get(ModelDescriptionConstants.RESULT).asString(), DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now())).toEpochMilli();
            Assert.assertEquals(time1, time2);

            operation.get(ATTRIBUTE).set("val");
            opRes = executeOperation(operation, GET_SESSION_ATTRIBUTE);
            Assert.assertEquals("1", opRes.get(ModelDescriptionConstants.RESULT).asString());

            executeOperation(operation, INVALIDATE_SESSION);

            opRes = executeOperation(operation, LIST_SESSIONS);
            Assert.assertEquals(Collections.emptyList(), opRes.get(ModelDescriptionConstants.RESULT).asList());
        }
    }

    @Test
    public void testSessionManagementOperationsNegative() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.parseCLIStyleAddress
                    ("/deployment=management.war/subsystem=undertow").toModelNode());

            String sessionId = "non-existing-id";

            operation.get(SESSION_ID).set(sessionId);

            negativeTestsCheck(operation, LIST_SESSION_ATTRIBUTE_NAMES);
            negativeTestsCheck(operation, LIST_SESSION_ATTRIBUTES);
            operation.get(ATTRIBUTE).set("val");
            negativeTestsCheck(operation, GET_SESSION_ATTRIBUTE);

            executeOperation(operation, INVALIDATE_SESSION);

            HttpGet get = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() +
                    ":8080/management/SessionPersistenceServlet");
            HttpResponse res = client.execute(get);
            sessionId = null;
            for (Header cookie : res.getHeaders("Set-Cookie")) {
                if (cookie.getValue().startsWith("JSESSIONID=")) {
                    sessionId = cookie.getValue().split("=")[1].split("\\.")[0];
                    break;
                }
            }
            operation.get(SESSION_ID).set(sessionId);
            operation.get(ATTRIBUTE).set("non-existing");

            ModelNode opRes = executeOperation(operation, GET_SESSION_ATTRIBUTE);
            Assert.assertEquals("undefined", opRes.get(ModelDescriptionConstants.RESULT).asString());

            // Invalidate created session.
            executeOperation(operation, INVALIDATE_SESSION);
        }
    }

    private ModelNode executeOperation(ModelNode operation, String operationName) throws IOException {
        operation.get(ModelDescriptionConstants.OP).set(operationName);

        ModelNode opRes = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());

        return opRes;
    }

    private void negativeTestsCheck(ModelNode operation, String operationName) throws IOException {
        operation.get(ModelDescriptionConstants.OP).set(operationName);

        ModelNode opRes = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(opRes.toString(), "failed", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
        String failDesc = opRes.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString();
        Assert.assertEquals("WFLYUT0100: Session non-existing-id not found", failDesc);
    }
}
