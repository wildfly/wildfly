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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BLOCKING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CONFIG_AS_XML_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacAdminCallbackHandler;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;

/**
 * Base class for RBAC tests.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public abstract class AbstractRbacTestCase {

    protected static final String AUTHORIZATION = "core-service=management/access=authorization";
    protected static final String DEPLOYMENT_1 = "deployment=war-example.war";
    protected static final String DEPLOYMENT_2 = "deployment=rbac.txt";
    protected static final byte[] DEPLOYMENT_2_CONTENT = "CONTENT".getBytes(Charset.defaultCharset());
    protected static final String TEST_PATH = "path=rbac.test";
    protected static final String MASTER = "master";
    protected static final String SLAVE = "slave";
    protected static final String SERVER_GROUP_A = "server-group-a";
    protected static final String SERVER_GROUP_B = "server-group-b";
    protected static final String MASTER_A = "master-a";
    protected static final String SLAVE_B = "slave-b";
    protected static final String SMALL_JVM = "jvm=small";
    protected static final String SCOPED_ROLE_SERVER = "server-config=scoped-role-server";

    private static final Map<String, String> SASL_OPTIONS = Collections.singletonMap("SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
    private static final String TEST = "test.war";
    private static final String REPLACEMENT = "test.war.v2";
    private static final String SECURITY_DOMAIN = "subsystem=1/rbac-sensitive=other";
    private static final String HTTP_BINDING = "socket-binding-group=sockets-a/socket-binding=http";
    private static final String MEMORY_MBEAN = "core-service=platform-mbean/type=memory";
    private static final String PROFILE_A = "profile=profile-a";
    private static final String EXAMPLE_CONSTRAINED = "subsystem=1/rbac-constrained=default";
    private static final String GENERIC_SERVER_CONFIG_ADDRESS = "host=master/server-config=*";

    private static final Map<String, ModelControllerClient> nonLocalAuthclients = new HashMap<String, ModelControllerClient>();
    private static final Map<String, ModelControllerClient> localAuthClients = new HashMap<String, ModelControllerClient>();
    protected static DomainTestSupport testSupport;
    protected static WildFlyManagedConfiguration masterClientConfig;

    @AfterClass
    public static void cleanUpClients() {

        try {
            cleanUpClients(nonLocalAuthclients);
        } finally {
            cleanUpClients(localAuthClients);
        }

    }

    private static void cleanUpClients(Map<String, ModelControllerClient> clients) {

        for (ModelControllerClient client : clients.values()) {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }

        clients.clear();
    }

    protected static void deployDeployment1(DomainClient domainClient) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_1, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);

        RbacUtil.executeOperation(domainClient, op, Outcome.SUCCESS);
    }

    protected static void removeDeployment1(DomainClient domainClient) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_1, REMOVE);
        RbacUtil.executeOperation(domainClient, op, Outcome.SUCCESS);
    }


    public ModelControllerClient getClientForUser(String userName, boolean allowLocalAuth,
                                                  WildFlyManagedConfiguration clientConfig) throws UnknownHostException {
        Map<String, ModelControllerClient> clients = allowLocalAuth ? localAuthClients : nonLocalAuthclients;
        ModelControllerClient result = clients.get(userName);
        if (result == null) {
            result = createClient(userName, allowLocalAuth, clientConfig);
            clients.put(userName, result);
        }
        return result;
    }

    private ModelControllerClient createClient(String userName, boolean allowLocalAuth,
                                               WildFlyManagedConfiguration clientConfig) throws UnknownHostException {

        return ModelControllerClient.Factory.create(clientConfig.getHostControllerManagementProtocol(),
                clientConfig.getHostControllerManagementAddress(),
                clientConfig.getHostControllerManagementPort(),
                new RbacAdminCallbackHandler(userName),
                allowLocalAuth ? Collections.<String, String>emptyMap() : SASL_OPTIONS);
    }

    public static void removeClientForUser(String userName, boolean allowLocalAuth) throws IOException {
        Map<String, ModelControllerClient> clients = allowLocalAuth ? localAuthClients : nonLocalAuthclients;
        ModelControllerClient client = clients.remove(userName);
        if (client != null) {
            client.close();
        }
    }

    protected abstract void configureRoles(ModelNode op, String[] roles);

    boolean readOnly = false; // used by RbacSoakTest

    /**
     * @param expectedOutcome for standard and host-scoped roles tests, this is the expected outcome of all operations;
     *                        for server-group-scoped roles tests, this is the expected outcome for the profile of the server group the user
     *                        is member of, as for the other profiles and for read-config-as-xml, the outcome is well known
     */
    protected void readWholeConfig(ModelControllerClient client, Outcome expectedOutcome, String... roles) throws IOException {
        Outcome expectedOutcomeForReadConfigAsXml = expectedOutcome;
        if (this instanceof AbstractServerGroupScopedRolesTestCase) {
            expectedOutcomeForReadConfigAsXml = Outcome.UNAUTHORIZED;
        }

        ModelNode op = createOpNode(null, READ_CONFIG_AS_XML_OPERATION);
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcomeForReadConfigAsXml);

        // the code below calls the non-published operation 'describe'; see WFLY-2379 for more info

        ModelControllerClient domainClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        op = createOpNode(null, READ_CHILDREN_NAMES_OPERATION);
        op.get(CHILD_TYPE).set(PROFILE);
        ModelNode profiles = RbacUtil.executeOperation(domainClient, op, Outcome.SUCCESS);
        for (ModelNode profile : profiles.get(RESULT).asList()) {
            Outcome expectedOutcomeForProfile = expectedOutcome;
            if (this instanceof AbstractServerGroupScopedRolesTestCase) {
                expectedOutcomeForProfile = "profile-a".equals(profile.asString()) ? expectedOutcome : Outcome.HIDDEN;
            }

            op = createOpNode("profile=" + profile.asString(), DESCRIBE);
            configureRoles(op, roles);
            ModelNode result = RbacUtil.executeOperation(client, op, expectedOutcomeForProfile);
            assertEquals(expectedOutcomeForProfile == Outcome.SUCCESS, result.hasDefined(RESULT));

            op = createOpNode("profile=" + profile.asString(), READ_CHILDREN_NAMES_OPERATION);
            op.get(CHILD_TYPE).set(SUBSYSTEM);
            ModelNode subsystems = RbacUtil.executeOperation(domainClient, op, Outcome.SUCCESS);
            for (ModelNode subsystem : subsystems.get(RESULT).asList()) {
                op = createOpNode("profile=" + profile.asString() + "/subsystem=" + subsystem.asString(), DESCRIBE);
                configureRoles(op, roles);
                result = RbacUtil.executeOperation(client, op, expectedOutcomeForProfile);
                assertEquals(expectedOutcomeForProfile == Outcome.SUCCESS, result.hasDefined(RESULT));
            }
        }
    }

    protected void checkStandardReads(ModelControllerClient client, String host, String server, String... roles) throws IOException {
        readResource(client, DEPLOYMENT_1, host, server, Outcome.SUCCESS, roles);
        readResource(client, HTTP_BINDING, host, server, Outcome.SUCCESS, roles);
    }

    protected ModelNode readResource(ModelControllerClient client, String address, String host, String server, Outcome expectedOutcome,
                                     String... roles) throws IOException {
        String serverPart = server == null ? "" : "/server=" + server;
        String fullAddress = host == null ? address : "host=" + host + serverPart + "/" + address;
        ModelNode op = createOpNode(fullAddress, READ_RESOURCE_OPERATION);
        configureRoles(op, roles);
        return RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    protected void checkRootRead(ModelControllerClient client, String host, String server, Outcome expectedOutcome, String... roles) throws IOException {
        String suffix = host == null ? null : "";
        readResource(client, suffix, host, server, expectedOutcome, roles);
    }

    protected void checkSecurityDomainRead(ModelControllerClient client, String host, String server, Outcome expectedOutcome, String... roles) throws IOException {
        String sdAddress = host == null ? PROFILE_A + "/" + SECURITY_DOMAIN : SECURITY_DOMAIN;
        readResource(client, sdAddress, host, server, expectedOutcome, roles);
    }

    protected void checkSensitiveAttribute(ModelControllerClient client, String host, String server, boolean expectSuccess, String... roles) throws IOException {
        String dsAddress = host == null ? PROFILE_A + "/" + EXAMPLE_CONSTRAINED
                : "host=" + host + "server=" + server + "/" + EXAMPLE_CONSTRAINED;
        ModelNode attrValue = readResource(client, dsAddress, host, server, Outcome.SUCCESS, roles).get(RESULT, PASSWORD);
        ModelNode correct = new ModelNode();
        if (expectSuccess) {
            correct.set("sa");
        }
        assertEquals(correct, attrValue);
    }

    protected void runGC(ModelControllerClient client, String host, String server, Outcome expectedOutcome, String... roles) throws IOException {
        String serverAddress = server == null ? "" : "/server=" + server;
        String fullAddress = "host=" + host + serverAddress + "/" + MEMORY_MBEAN;
        ModelNode op = createOpNode(fullAddress, "gc");
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    protected void addDeployment2(ModelControllerClient client, Outcome expectedOutcome, String... roles) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_2, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    protected void addPath(ModelControllerClient client, Outcome expectedOutcome, String... roles) throws IOException {
        ModelNode op = createOpNode(TEST_PATH, ADD);
        op.get(PATH).set("/");
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    protected static String getPrefixedAddress(String prefixKey, String prefixValue, String address) {
        return prefixKey + "=" + prefixValue + "/" + address;
    }

    protected void addJvm(ModelControllerClient client, String prefixKey, String prefixValue,
                          Outcome expectedOutcome, String... roles) throws IOException {
        String fullAddress = getPrefixedAddress(prefixKey, prefixValue, SMALL_JVM);
        ModelNode op = createOpNode(fullAddress, ADD);
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    protected void addServerConfig(ModelControllerClient client, String host, String serverGroup,
                          Outcome expectedOutcome, String... roles) throws IOException {
        String fullAddress = getPrefixedAddress(HOST, host, SCOPED_ROLE_SERVER);
        ModelNode op = createOpNode(fullAddress, ADD);
        op.get(GROUP).set(serverGroup);
        op.get(AUTO_START).set(false);
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    protected void restartServer(ModelControllerClient client, String host, String server,
                                 Outcome expectedOutcome, String... roles) throws IOException {
        String fullAddress = String.format("host=%s/server-config=%s", host, server);
        ModelNode op = createOpNode(fullAddress, RESTART);
        op.get(BLOCKING).set(true);
        configureRoles(op, roles);
        RbacUtil.executeOperation(client, op, expectedOutcome);
    }

    protected ModelNode getServerConfigAccessControl(ModelControllerClient client, String... roles) throws IOException {
        ModelNode op = createOpNode(GENERIC_SERVER_CONFIG_ADDRESS, READ_RESOURCE_DESCRIPTION_OPERATION);
        op.get(ACCESS_CONTROL).set("trim-descriptions");
        op.get(OPERATIONS).set(true);
        configureRoles(op, roles);
        return RbacUtil.executeOperation(client, op, Outcome.SUCCESS).get(RESULT).get(0).get(RESULT, ACCESS_CONTROL);
    }

    protected void removeResource(String address) throws IOException {
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
