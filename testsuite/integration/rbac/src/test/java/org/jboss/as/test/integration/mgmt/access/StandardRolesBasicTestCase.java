/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.mgmt.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.ADMINISTRATOR_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.AUDITOR_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.DEPLOYER_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.MAINTAINER_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.MONITOR_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.OPERATOR_USER;
import static org.jboss.as.test.integration.mgmt.access.util.RbacUtil.SUPERUSER_USER;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.mgmt.access.util.Outcome;
import org.jboss.as.test.integration.mgmt.access.util.RbacUtil;
import org.jboss.as.test.integration.mgmt.access.util.UserRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic tests of the standard RBAC roles.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(UserRolesMappingServerSetupTask.StandardUsersSetup.class)
public class StandardRolesBasicTestCase extends AbstractRbacTestCase {

    private static final String DEPLOYMENT_1 = "deployment=war-example.war";
    private static final String DEPLOYMENT_2 = "deployment=rbac.txt";
    private static final byte[] DEPLOYMENT_2_CONTENT = "CONTENT".getBytes(Charset.defaultCharset());
    private static final String MANAGEMENT_REALM = "core-service=management/security-realm=ManagementRealm";
    private static final String HTTP_BINDING = "socket-binding-group=standard-sockets/socket-binding=http";
    private static final String MEMORY_MBEAN = "core-service=platform-mbean/type=memory";
    private static final String EXAMPLE_DS = "subsystem=datasources/data-source=ExampleDS";
    private static final String TEST_PATH = "path=rbac.test";

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        // Tired of fighting Intellij to get it to pick up a file to include in the war so I can debug, I resort to...
        final String html = "<html><body>Hello</body></html>";
        war.addAsWebResource(new Asset() {
            @Override
            public InputStream openStream() {
                try {
                    return new ByteArrayInputStream(html.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "index.html");
        return war;
    }

    @After
    public void tearDown() throws IOException {
        AssertionError assertionError = null;
        try {
            removeResource(DEPLOYMENT_2);
        } catch (AssertionError e) {
            assertionError = e;
        } finally {
            removeResource(TEST_PATH);
        }


        if (assertionError != null) {
            throw assertionError;
        }
    }

    @Test
    public void testMonitor() throws Exception {
        ModelControllerClient client = getClientForUser(MONITOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.UNAUTHORIZED);
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testOperator() throws Exception {
        ModelControllerClient client = getClientForUser(OPERATOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testMaintainer() throws Exception {
        ModelControllerClient client = getClientForUser(MAINTAINER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);
    }

    @Test
    public void testDeployer() throws Exception {
        ModelControllerClient client = getClientForUser(DEPLOYER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.UNAUTHORIZED);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testAdministrator() throws Exception {
        ModelControllerClient client = getClientForUser(ADMINISTRATOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);
    }

    @Test
    public void testAuditor() throws Exception {
        ModelControllerClient client = getClientForUser(AUDITOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        runGC(client, Outcome.UNAUTHORIZED);
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testSuperUser() throws Exception {
        ModelControllerClient client = getClientForUser(SUPERUSER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);
    }

    private static void checkStandardReads(ModelControllerClient client) throws IOException {
        readResource(client, null, Outcome.SUCCESS);
        readResource(client, DEPLOYMENT_1, Outcome.SUCCESS);
        readResource(client, HTTP_BINDING, Outcome.SUCCESS);
    }

    private static ModelNode readResource(ModelControllerClient client, String address, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(address, READ_RESOURCE_OPERATION);

        return RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static void checkSensitiveAttribute(ModelControllerClient client, boolean expectSuccess) throws IOException {
        ModelNode attrValue = readResource(client, EXAMPLE_DS, Outcome.SUCCESS).get(RESULT, PASSWORD);
        ModelNode correct = new ModelNode();
        if (expectSuccess) {
            correct.set("sa");
        }
        assertEquals(correct, attrValue);
    }

    private static void runGC(ModelControllerClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(MEMORY_MBEAN, "gc");
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static void addDeployment2(ModelControllerClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_2, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);

        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static void addPath(ModelControllerClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(TEST_PATH, ADD);
        op.get(PATH).set("/");
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private void removeResource(String address) throws IOException {
        ModelNode op = createOpNode(address, READ_RESOURCE_OPERATION);
        ModelNode result = getManagementClient().getControllerClient().execute(op);
        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
            op = createOpNode(address, REMOVE);
            result = getManagementClient().getControllerClient().execute(op);
            assertEquals(result.asString(), SUCCESS, result.get(OUTCOME).asString());
        }

    }

}
