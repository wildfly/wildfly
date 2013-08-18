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

package org.jboss.as.test.integration.domain.rbac;

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
import static org.jboss.as.test.integration.management.rbac.RbacUtil.ADMINISTRATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.AUDITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.DEPLOYER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.OPERATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_USER;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.Charset;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * Abstract superclass of access control provider test cases.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public abstract class AbstractStandardRolesTestCase extends AbstractRbacTestCase {

    protected static final String DEPLOYMENT_1 = "deployment=war-example.war";
    protected static final String DEPLOYMENT_2 = "deployment=rbac.txt";
    protected static final byte[] DEPLOYMENT_2_CONTENT = "CONTENT".getBytes(Charset.defaultCharset());
    protected static final String TEST_PATH = "path=rbac.test";
    private static final String TEST = "test.war";
    private static final String REPLACEMENT = "test.war.v2";
    private static final String MASTER = "master";
    private static final String SLAVE = "slave";
    private static final String MAIN_ONE = "main-one";
    private static final String SECURITY_DOMAIN = "subsystem=security/security-domain=other";
    private static final String HTTP_BINDING = "socket-binding-group=standard-sockets/socket-binding=http";
    private static final String MEMORY_MBEAN = "core-service=platform-mbean/type=memory";
    private static final String DEFAULT_PROFILE = "profile=default";
    private static final String EXAMPLE_DS = "subsystem=datasources/data-source=ExampleDS";
    protected static DomainTestSupport testSupport;
    protected static JBossAsManagedConfiguration masterClientConfig;


    protected static void deployDeployment1(DomainClient domainClient) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_1, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);

        RbacUtil.executeOperation(domainClient, op, Outcome.SUCCESS);
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

    protected abstract boolean isAllowLocalAuth();

    protected abstract void configureRoles(ModelNode op, String[] roles);

    @Test
    public void testMonitor() throws Exception {
        ModelControllerClient client = getClientForUser(MONITOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, MONITOR_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, MONITOR_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, MONITOR_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, MONITOR_USER);
        checkSecurityDomainRead(client, null, null, Outcome.HIDDEN, MONITOR_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.HIDDEN, MONITOR_USER);
        checkSensitiveAttribute(client, null, null, false, MONITOR_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, false, MONITOR_USER);
        runGC(client, MASTER, null, Outcome.UNAUTHORIZED, MONITOR_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.UNAUTHORIZED, MONITOR_USER);
        addDeployment2(client, Outcome.UNAUTHORIZED, MONITOR_USER);
        addPath(client, Outcome.UNAUTHORIZED, MONITOR_USER);
    }

    @Test
    public void testOperator() throws Exception {
        ModelControllerClient client = getClientForUser(OPERATOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, OPERATOR_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, OPERATOR_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, OPERATOR_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, OPERATOR_USER);
        checkSecurityDomainRead(client, null, null, Outcome.HIDDEN, OPERATOR_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.HIDDEN, OPERATOR_USER);
        checkSensitiveAttribute(client, null, null, false, OPERATOR_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, false, OPERATOR_USER);
        runGC(client, MASTER, null, Outcome.SUCCESS, OPERATOR_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.SUCCESS, OPERATOR_USER);
        addDeployment2(client, Outcome.UNAUTHORIZED, OPERATOR_USER);
        addPath(client, Outcome.UNAUTHORIZED, OPERATOR_USER);
    }

    @Test
    public void testMaintainer() throws Exception {
        ModelControllerClient client = getClientForUser(MAINTAINER_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, MAINTAINER_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, MAINTAINER_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, MAINTAINER_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, MAINTAINER_USER);
        checkSecurityDomainRead(client, null, null, Outcome.HIDDEN, MAINTAINER_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.HIDDEN, MAINTAINER_USER);
        checkSensitiveAttribute(client, null, null, false, MAINTAINER_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, false, MAINTAINER_USER);
        runGC(client, MASTER, null, Outcome.SUCCESS, MAINTAINER_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.SUCCESS, MAINTAINER_USER);
        addDeployment2(client, Outcome.SUCCESS, MAINTAINER_USER);
        addPath(client, Outcome.SUCCESS, MAINTAINER_USER);
    }

    @Test
    public void testDeployer() throws Exception {
        ModelControllerClient client = getClientForUser(DEPLOYER_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, DEPLOYER_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, DEPLOYER_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, DEPLOYER_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, DEPLOYER_USER);
        checkSecurityDomainRead(client, null, null, Outcome.HIDDEN, DEPLOYER_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.HIDDEN, DEPLOYER_USER);
        checkSensitiveAttribute(client, null, null, false, DEPLOYER_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, false, DEPLOYER_USER);
        runGC(client, MASTER, null, Outcome.UNAUTHORIZED, DEPLOYER_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.UNAUTHORIZED, DEPLOYER_USER);
        addDeployment2(client, Outcome.SUCCESS, DEPLOYER_USER);
        addPath(client, Outcome.UNAUTHORIZED, DEPLOYER_USER);
    }

    @Test
    public void testAdministrator() throws Exception {
        ModelControllerClient client = getClientForUser(ADMINISTRATOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, ADMINISTRATOR_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkSecurityDomainRead(client, null, null, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkSensitiveAttribute(client, null, null, true, ADMINISTRATOR_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, true, ADMINISTRATOR_USER);
        runGC(client, MASTER, null, Outcome.SUCCESS, ADMINISTRATOR_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.SUCCESS, ADMINISTRATOR_USER);
        addDeployment2(client, Outcome.SUCCESS, ADMINISTRATOR_USER);
        addPath(client, Outcome.SUCCESS, ADMINISTRATOR_USER);
    }

    @Test
    public void testAuditor() throws Exception {
        ModelControllerClient client = getClientForUser(AUDITOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, AUDITOR_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, AUDITOR_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, AUDITOR_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, AUDITOR_USER);
        checkSecurityDomainRead(client, null, null, Outcome.SUCCESS, AUDITOR_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, AUDITOR_USER);
        checkSensitiveAttribute(client, null, null, true, AUDITOR_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, true, AUDITOR_USER);
        runGC(client, MASTER, null, Outcome.UNAUTHORIZED, AUDITOR_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.UNAUTHORIZED, AUDITOR_USER);
        addDeployment2(client, Outcome.UNAUTHORIZED, AUDITOR_USER);
        addPath(client, Outcome.UNAUTHORIZED, AUDITOR_USER);
    }

    @Test
    public void testSuperUser() throws Exception {
        ModelControllerClient client = getClientForUser(SUPERUSER_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, null, null, SUPERUSER_USER);
        checkRootRead(client, null, null, Outcome.SUCCESS, SUPERUSER_USER);
        checkRootRead(client, MASTER, null, Outcome.SUCCESS, SUPERUSER_USER);
        checkRootRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, SUPERUSER_USER);
        checkSecurityDomainRead(client, null, null, Outcome.SUCCESS, SUPERUSER_USER);
        checkSecurityDomainRead(client, MASTER, MAIN_ONE, Outcome.SUCCESS, SUPERUSER_USER);
        checkSensitiveAttribute(client, null, null, true, SUPERUSER_USER);
        checkSensitiveAttribute(client, MASTER, MAIN_ONE, true, SUPERUSER_USER);
        runGC(client, MASTER, null, Outcome.SUCCESS, SUPERUSER_USER);
        runGC(client, MASTER, MAIN_ONE, Outcome.SUCCESS, SUPERUSER_USER);
        addDeployment2(client, Outcome.SUCCESS, SUPERUSER_USER);
        addPath(client, Outcome.SUCCESS, SUPERUSER_USER);
    }

    private void checkStandardReads(ModelControllerClient client, String host, String server, String... roles) throws IOException {
        readResource(client, DEPLOYMENT_1, host, server, Outcome.SUCCESS, roles);
        readResource(client, HTTP_BINDING, host, server, Outcome.SUCCESS, roles);
    }

    private ModelNode readResource(ModelControllerClient client, String address, String host, String server, Outcome expectedOutcome,
                                          String... roles) throws IOException {
        String serverPart = server == null ? "" : "/server=" + server;
        String fullAddress = host == null ? address : "host=" + host + serverPart + "/" + address;
        ModelNode op = createOpNode(fullAddress, READ_RESOURCE_OPERATION);
        configureRoles(op, roles);
        return RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private void checkRootRead(ModelControllerClient client, String host, String server, Outcome expectedOutcome, String... roles) throws IOException {
        String suffix = host == null ? null : "";
        readResource(client, suffix, host, server, expectedOutcome, roles);
    }

    private void checkSecurityDomainRead(ModelControllerClient client, String host, String server, Outcome expectedOutcome, String... roles) throws IOException {
        String sdAddress = host == null ? DEFAULT_PROFILE + "/" + SECURITY_DOMAIN : SECURITY_DOMAIN;
        readResource(client, sdAddress, host, server, expectedOutcome, roles);
    }

    private void checkSensitiveAttribute(ModelControllerClient client, String host, String server, boolean expectSuccess, String... roles) throws IOException {
        String dsAddress = host == null ? DEFAULT_PROFILE + "/" + EXAMPLE_DS
                : "host=" + host + "server=" + server + "/" + EXAMPLE_DS;
        ModelNode attrValue = readResource(client, dsAddress, host, server, Outcome.SUCCESS, roles).get(RESULT, PASSWORD);
        ModelNode correct = new ModelNode();
        if (expectSuccess) {
            correct.set("sa");
        }
        assertEquals(correct, attrValue);
    }

    private void runGC(ModelControllerClient client, String host, String server, Outcome expectedOutcome, String... roles) throws IOException {
        String serverAddress = server == null ? "" : "/server=" + server;
        String fullAddress = "host=" + host + serverAddress + "/" + MEMORY_MBEAN;
        ModelNode op = createOpNode(fullAddress, "gc");
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private void addDeployment2(ModelControllerClient client, Outcome expectedOutcome, String... roles) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_2, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private void addPath(ModelControllerClient client, Outcome expectedOutcome, String... roles) throws IOException {
        ModelNode op = createOpNode(TEST_PATH, ADD);
        op.get(PATH).set("/");
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private void removeResource(String address) throws IOException {
        ModelNode op = createOpNode(address, READ_RESOURCE_OPERATION);
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        ModelNode result = domainClient.execute(op);
        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
            op = createOpNode(address, REMOVE);
            result = domainClient.execute(op);
            assertEquals(result.asString(), SUCCESS, result.get(OUTCOME).asString());
        }

    }
}
