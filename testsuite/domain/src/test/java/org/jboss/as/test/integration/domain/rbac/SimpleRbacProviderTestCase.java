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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLES;
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
import java.net.URL;
import java.nio.charset.Charset;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfigurationParameters;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests of the "simple" provider for RBAC.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SimpleRbacProviderTestCase extends AbstractRbacTestCase {

    private static final String TEST = "test.war";
    private static final String REPLACEMENT = "test.war.v2";

    private static final String DEPLOYMENT_1 = "deployment=war-example.war";
    private static final String DEPLOYMENT_2 = "deployment=rbac.txt";
    private static final byte[] DEPLOYMENT_2_CONTENT = "CONTENT".getBytes(Charset.defaultCharset());

    private static final String SECURITY_DOMAIN = "profile=default/subsystem=security/security-domain=other";
    private static final String HTTP_BINDING = "socket-binding-group=standard-sockets/socket-binding=http";
    private static final String MEMORY_MBEAN = "host=master/core-service=platform-mbean/type=memory";
    private static final String EXAMPLE_DS = "profile=default/subsystem=datasources/data-source=ExampleDS";
    private static final String TEST_PATH = "path=rbac.test";

    private static DomainTestSupport testSupport;
    private static JBossAsManagedConfiguration masterClientConfig;
    private static WebArchive webArchive;
    private static WebArchive webArchive2;

    @BeforeClass
    public static void setupDomain() throws Exception {

        // Create our deployments
        webArchive = ShrinkWrap.create(WebArchive.class, TEST);
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL index = tccl.getResource("helloWorld/index.html");
        webArchive.addAsWebResource(index, "index.html");

        webArchive2 = ShrinkWrap.create(WebArchive.class, TEST);
        index = tccl.getResource("helloWorld/index.html");
        webArchive2.addAsWebResource(index, "index.html");
        index = tccl.getResource("helloWorld/index2.html");
        webArchive2.addAsWebResource(index, "index2.html");

        // Launch the domain

        // TODO use DomainTestSuite once config propagation to slaves is sorted
//        testSupport = DomainTestSuite.createSupport(SimpleRbacProviderTestCase.class.getSimpleName());
        final DomainTestSupport.Configuration config = DomainTestSupport.Configuration.create("domain-configs/domain-standard.xml", "host-configs/host-master.xml", null, JBossAsManagedConfigurationParameters.STANDARD, null);
        testSupport = DomainTestSupport.createAndStartSupport(SimpleRbacProviderTestCase.class.getSimpleName(), config);
        masterClientConfig = testSupport.getDomainMasterConfiguration();
        DomainClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.setup(domainClient);


        ModelNode op = createOpNode(DEPLOYMENT_1, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);

        RbacUtil.executeOperation(domainClient, op, Outcome.SUCCESS);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {

        try {
            UserRolesMappingServerSetupTask.StandardUsersSetup.INSTANCE.tearDown(testSupport.getDomainMasterLifecycleUtil().getDomainClient());
        } finally {
            // TODO use DomainTestSuite once config propagation to slaves is sorted
//            testSupport = null;
//            DomainTestSuite.stopSupport();
            testSupport.stop();
            testSupport = null;
        }
    }

    protected boolean isAllowLocalAuth() {
        return true;
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
        ModelControllerClient client = getClientForUser(MONITOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, MONITOR_USER);
        readResource(client, SECURITY_DOMAIN, Outcome.HIDDEN, MONITOR_USER);
        checkSensitiveAttribute(client, false, MONITOR_USER);
        runGC(client, Outcome.UNAUTHORIZED, MONITOR_USER);
        addDeployment2(client, Outcome.UNAUTHORIZED, MONITOR_USER);
        addPath(client, Outcome.UNAUTHORIZED, MONITOR_USER);
    }

    @Test
    public void testOperator() throws Exception {
        ModelControllerClient client = getClientForUser(OPERATOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, OPERATOR_USER);
        readResource(client, SECURITY_DOMAIN, Outcome.HIDDEN, OPERATOR_USER);
        checkSensitiveAttribute(client, false, OPERATOR_USER);
        runGC(client, Outcome.SUCCESS, OPERATOR_USER);
        addDeployment2(client, Outcome.UNAUTHORIZED, OPERATOR_USER);
        addPath(client, Outcome.UNAUTHORIZED, OPERATOR_USER);
    }

    @Test
    public void testMaintainer() throws Exception {
        ModelControllerClient client = getClientForUser(MAINTAINER_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, MAINTAINER_USER);
        readResource(client, SECURITY_DOMAIN, Outcome.HIDDEN, MAINTAINER_USER);
        checkSensitiveAttribute(client, false, MAINTAINER_USER);
        runGC(client, Outcome.SUCCESS, MAINTAINER_USER);
        addDeployment2(client, Outcome.SUCCESS, MAINTAINER_USER);
        addPath(client, Outcome.SUCCESS, MAINTAINER_USER);
    }

    @Test
    public void testDeployer() throws Exception {
        ModelControllerClient client = getClientForUser(DEPLOYER_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, DEPLOYER_USER);
        readResource(client, SECURITY_DOMAIN, Outcome.HIDDEN, DEPLOYER_USER);
        checkSensitiveAttribute(client, false, DEPLOYER_USER);
        runGC(client, Outcome.UNAUTHORIZED, DEPLOYER_USER);
        addDeployment2(client, Outcome.SUCCESS, DEPLOYER_USER);
        addPath(client, Outcome.UNAUTHORIZED, DEPLOYER_USER);
    }

    @Test
    public void testAdministrator() throws Exception {
        ModelControllerClient client = getClientForUser(ADMINISTRATOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, ADMINISTRATOR_USER);
        readResource(client, SECURITY_DOMAIN, Outcome.SUCCESS, ADMINISTRATOR_USER);
        checkSensitiveAttribute(client, true, ADMINISTRATOR_USER);
        runGC(client, Outcome.SUCCESS, ADMINISTRATOR_USER);
        addDeployment2(client, Outcome.SUCCESS, ADMINISTRATOR_USER);
        addPath(client, Outcome.SUCCESS, ADMINISTRATOR_USER);
    }

    @Test
    public void testAuditor() throws Exception {
        ModelControllerClient client = getClientForUser(AUDITOR_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, AUDITOR_USER);
        readResource(client, SECURITY_DOMAIN, Outcome.SUCCESS, AUDITOR_USER);
        checkSensitiveAttribute(client, true, AUDITOR_USER);
        runGC(client, Outcome.UNAUTHORIZED, AUDITOR_USER);
        addDeployment2(client, Outcome.UNAUTHORIZED, AUDITOR_USER);
        addPath(client, Outcome.UNAUTHORIZED, AUDITOR_USER);
    }

    @Test
    public void testSuperUser() throws Exception {
        ModelControllerClient client = getClientForUser(SUPERUSER_USER, isAllowLocalAuth(), masterClientConfig);
        checkStandardReads(client, SUPERUSER_USER);
        readResource(client, SECURITY_DOMAIN, Outcome.SUCCESS, SUPERUSER_USER);
        checkSensitiveAttribute(client, true, SUPERUSER_USER);
        runGC(client, Outcome.SUCCESS, SUPERUSER_USER);
        addDeployment2(client, Outcome.SUCCESS, SUPERUSER_USER);
        addPath(client, Outcome.SUCCESS, SUPERUSER_USER);
    }

    private static void checkStandardReads(ModelControllerClient client, String... roles) throws IOException {
        readResource(client, null, Outcome.SUCCESS, roles);
        readResource(client, DEPLOYMENT_1, Outcome.SUCCESS, roles);
        readResource(client, HTTP_BINDING, Outcome.SUCCESS, roles);
    }

    private static ModelNode readResource(ModelControllerClient client, String address, Outcome expectedOutcome,
                                          String... roles) throws IOException {
        ModelNode op = createOpNode(address, READ_RESOURCE_OPERATION);
        configureRoles(op, roles);
        return RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static void configureRoles(ModelNode op, String[] roles) {
        ModelNode rolesNode = op.get(OPERATION_HEADERS, ROLES);
        for (String role : roles) {
            rolesNode.add(role);
        }
    }

    private static void checkSensitiveAttribute(ModelControllerClient client, boolean expectSuccess, String... roles) throws IOException {
        ModelNode attrValue = readResource(client, EXAMPLE_DS, Outcome.SUCCESS, roles).get(RESULT, PASSWORD);
        ModelNode correct = new ModelNode();
        if (expectSuccess) {
            correct.set("sa");
        }
        assertEquals(correct, attrValue);
    }

    private static void runGC(ModelControllerClient client, Outcome expectedOutcome, String... roles) throws IOException {
        ModelNode op = createOpNode(MEMORY_MBEAN, "gc");
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static void addDeployment2(ModelControllerClient client, Outcome expectedOutcome, String... roles) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_2, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    private static void addPath(ModelControllerClient client, Outcome expectedOutcome, String... roles) throws IOException {
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
