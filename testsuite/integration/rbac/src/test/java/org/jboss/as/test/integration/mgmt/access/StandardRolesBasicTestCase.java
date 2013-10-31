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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CONFIG_AS_XML_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.ADMINISTRATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.AUDITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.DEPLOYER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.OPERATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_USER;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.interfaces.ManagementInterface;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;

/**
 * Basic tests of the standard RBAC roles.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public abstract class StandardRolesBasicTestCase extends AbstractManagementInterfaceRbacTestCase {

    private static final String DEPLOYMENT_1 = "deployment=war-example.war";
    private static final String DEPLOYMENT_2 = "deployment=rbac.txt";
    private static final byte[] DEPLOYMENT_2_CONTENT = "CONTENT".getBytes(Charset.defaultCharset());
    private static final String AUTHORIZATION = "core-service=management/access=authorization";
    private static final String ROLE_MAPPING_BASE = "core-service=management/access=authorization/role-mapping=";
    private static final String MANAGEMENT_REALM = "core-service=management/security-realm=ManagementRealm";
    private static final String HTTP_BINDING = "socket-binding-group=standard-sockets/socket-binding=http";
    private static final String MEMORY_MBEAN = "core-service=platform-mbean/type=memory";
    private static final String EXAMPLE_DS = "subsystem=datasources/data-source=ExampleDS";
    private static final String TEST_PATH = "path=rbac.test";

    private static final ModelNode WFLY_1916_OP;
    static {
        WFLY_1916_OP = Util.createEmptyOperation(UPLOAD_DEPLOYMENT_BYTES, PathAddress.EMPTY_ADDRESS);
        WFLY_1916_OP.get(BYTES).set(new byte[64]);

        WFLY_1916_OP.protect();
    }
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
        ManagementInterface client = getClientForUser(MONITOR_USER);
        whoami(client, MONITOR_USER);
        readWholeConfig(client, Outcome.UNAUTHORIZED);
        checkStandardReads(client);
        readResource(client, AUTHORIZATION, Outcome.HIDDEN);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.UNAUTHORIZED);
        if (this instanceof JmxInterfaceStandardRolesBasicTestCase) {
            return; // the 'add' operation is not implemented in JmxManagementInterface
        }
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);

        testWFLY1916(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testOperator() throws Exception {
        ManagementInterface client = getClientForUser(OPERATOR_USER);
        whoami(client, OPERATOR_USER);
        readWholeConfig(client, Outcome.UNAUTHORIZED);
        checkStandardReads(client);
        readResource(client, AUTHORIZATION, Outcome.HIDDEN);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.SUCCESS);
        if (this instanceof JmxInterfaceStandardRolesBasicTestCase) {
            return; // the 'add' operation is not implemented in JmxManagementInterface
        }
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);

        testWFLY1916(client, Outcome.SUCCESS);
    }

    @Test
    public void testMaintainer() throws Exception {
        ManagementInterface client = getClientForUser(MAINTAINER_USER);
        whoami(client, MAINTAINER_USER);
        readWholeConfig(client, Outcome.UNAUTHORIZED);
        checkStandardReads(client);
        readResource(client, AUTHORIZATION, Outcome.HIDDEN);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.SUCCESS);
        if (this instanceof JmxInterfaceStandardRolesBasicTestCase) {
            return; // the 'add' operation is not implemented in JmxManagementInterface
        }
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);

        testWFLY1916(client, Outcome.SUCCESS);
    }

    @Test
    public void testDeployer() throws Exception {
        ManagementInterface client = getClientForUser(DEPLOYER_USER);
        whoami(client, DEPLOYER_USER);
        readWholeConfig(client, Outcome.UNAUTHORIZED);
        checkStandardReads(client);
        readResource(client, AUTHORIZATION, Outcome.HIDDEN);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.UNAUTHORIZED);
        if (this instanceof JmxInterfaceStandardRolesBasicTestCase) {
            return; // the 'add' operation is not implemented in JmxManagementInterface
        }
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.UNAUTHORIZED);

        testWFLY1916(client, Outcome.SUCCESS);
    }

    @Test
    public void testAdministrator() throws Exception {
        ManagementInterface client = getClientForUser(ADMINISTRATOR_USER);
        whoami(client, ADMINISTRATOR_USER);
        readWholeConfig(client, Outcome.SUCCESS);
        checkStandardReads(client);
        readResource(client, AUTHORIZATION, Outcome.SUCCESS);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        runGC(client, Outcome.SUCCESS);
        if (this instanceof JmxInterfaceStandardRolesBasicTestCase) {
            return; // the 'add' operation is not implemented in JmxManagementInterface
        }
        modifyAccessibleRoles(client, RbacUtil.MONITOR_ROLE, RbacUtil.OPERATOR_ROLE, RbacUtil.MAINTAINER_ROLE, RbacUtil.ADMINISTRATOR_ROLE, RbacUtil.DEPLOYER_ROLE);
        modifyInaccessibleRoles(client, RbacUtil.AUDITOR_ROLE, RbacUtil.SUPERUSER_ROLE);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);

        testWFLY1916(client, Outcome.SUCCESS);
    }

    @Test
    public void testAuditor() throws Exception {
        ManagementInterface client = getClientForUser(AUDITOR_USER);
        whoami(client, AUDITOR_USER);
        readWholeConfig(client, Outcome.SUCCESS);
        checkStandardReads(client);
        readResource(client, AUTHORIZATION, Outcome.SUCCESS);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        runGC(client, Outcome.UNAUTHORIZED);
        if (this instanceof JmxInterfaceStandardRolesBasicTestCase) {
            return; // the 'add' operation is not implemented in JmxManagementInterface
        }
        modifyInaccessibleRoles(client, RbacUtil.allStandardRoles());
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);

        testWFLY1916(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testSuperUser() throws Exception {
        ManagementInterface client = getClientForUser(SUPERUSER_USER);
        whoami(client, SUPERUSER_USER);
        readWholeConfig(client, Outcome.SUCCESS);
        checkStandardReads(client);
        readResource(client, AUTHORIZATION, Outcome.SUCCESS);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        runGC(client, Outcome.SUCCESS);
        if (this instanceof JmxInterfaceStandardRolesBasicTestCase) {
            return; // the 'add' operation is not implemented in JmxManagementInterface
        }
        modifyAccessibleRoles(client, RbacUtil.allStandardRoles());
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);

        testWFLY1916(client, Outcome.SUCCESS);
    }

    private static void whoami(ManagementInterface client, String expectedUsername) throws IOException {
        ModelNode op = createOpNode(null, "whoami");
        ModelNode result = RbacUtil.executeOperation(client, op, Outcome.SUCCESS);
        String returnedUsername = result.get(RESULT, "identity", USERNAME).asString();
        assertEquals(expectedUsername, returnedUsername);
    }

    private void readWholeConfig(ManagementInterface client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(null, READ_CONFIG_AS_XML_OPERATION);
        RbacUtil.executeOperation(client, op, expectedOutcome);

        // the code below calls the non-published operation 'describe'; see WFLY-2379 for more info
        // once that issue is fixed, the test will only make sense for native mgmt interface
        // (or maybe not even that)

        if (this instanceof JmxInterfaceStandardRolesBasicTestCase) {
            return;
        }

        op = createOpNode(null, READ_CHILDREN_NAMES_OPERATION);
        op.get(CHILD_TYPE).set(SUBSYSTEM);
        ModelNode subsystems = RbacUtil.executeOperation(getManagementClient().getControllerClient(), op, Outcome.SUCCESS);
        for (ModelNode subsystem : subsystems.get(RESULT).asList()) {
            op = createOpNode("subsystem=" + subsystem.asString(), DESCRIBE);
            ModelNode result = RbacUtil.executeOperation(client, op, expectedOutcome);
            assertEquals(expectedOutcome == Outcome.SUCCESS, result.hasDefined(RESULT));
        }
    }

    private static void checkStandardReads(ManagementInterface client) throws IOException {
        readResource(client, null, Outcome.SUCCESS);
        readResource(client, DEPLOYMENT_1, Outcome.SUCCESS);
        readResource(client, HTTP_BINDING, Outcome.SUCCESS);
    }

    private static ModelNode readResource(ManagementInterface client, String address, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(address, READ_RESOURCE_OPERATION);

        return RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static ModelNode readAttribute(ManagementInterface client, String address, String attributeName,
                                           Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(address, READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attributeName);

        return RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static void checkSensitiveAttribute(ManagementInterface client, boolean expectSuccess) throws IOException {
        ModelNode correct = new ModelNode();
        if (expectSuccess) {
            correct.set("sa");
        }

        ModelNode attrValue = readResource(client, EXAMPLE_DS, Outcome.SUCCESS).get(RESULT, PASSWORD);
        assertEquals(correct, attrValue);

        attrValue = readAttribute(client, EXAMPLE_DS, PASSWORD, expectSuccess ? Outcome.SUCCESS : Outcome.UNAUTHORIZED).get(RESULT);
        assertEquals(correct, attrValue);
    }

    private static void runGC(ManagementInterface client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(MEMORY_MBEAN, "gc");
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static void addDeployment2(ManagementInterface client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_2, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);

        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static void addPath(ManagementInterface client, Outcome expectedOutcome) throws IOException {
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

    private static void modifyAccessibleRoles(ManagementInterface client, String... roleNames) throws IOException {
        for (String current : roleNames) {
            addRemoveIncldueForRole(client, current, true);
        }
    }

    private static void modifyInaccessibleRoles(ManagementInterface client, String ... roleNames) throws IOException {
        for (String current : roleNames) {
            addRemoveIncldueForRole(client, current, false);
        }
    }

    private static void addRemoveIncldueForRole(final ManagementInterface client, final String roleName, boolean accessible) throws IOException {
        String includeAddress = ROLE_MAPPING_BASE + roleName + "/include=temp";
        ModelNode add = createOpNode(includeAddress, ADD);
        add.get(NAME).set("temp");
        add.get(TYPE).set(USER);

        RbacUtil.executeOperation(client, add, accessible ? Outcome.SUCCESS : Outcome.UNAUTHORIZED);

        if (accessible) {
            ModelNode remove = createOpNode(includeAddress, REMOVE);
            RbacUtil.executeOperation(client, remove, Outcome.SUCCESS);
        }
    }

    private void testWFLY1916(ManagementInterface client, Outcome expected) throws IOException {
        ModelNode op = WFLY_1916_OP.clone();
        RbacUtil.executeOperation(client, op, expected);
    }

}
