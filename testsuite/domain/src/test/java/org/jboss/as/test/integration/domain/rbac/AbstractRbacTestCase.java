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
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
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

    protected static final String DEPLOYMENT_1 = "deployment=war-example.war";
    protected static final String DEPLOYMENT_2 = "deployment=rbac.txt";
    protected static final byte[] DEPLOYMENT_2_CONTENT = "CONTENT".getBytes(Charset.defaultCharset());
    protected static final String TEST_PATH = "path=rbac.test";
    protected static final String MASTER = "master";
    protected static final String MAIN_SERVER_GROUP = "main-server-group";
    protected static final String MAIN_ONE = "main-one";
    protected static final String OTHER_ONE = "other-one";
    private static final Map<String, ModelControllerClient> nonLocalAuthclients = new HashMap<String, ModelControllerClient>();
    private static final Map<String, ModelControllerClient> localAuthClients = new HashMap<String, ModelControllerClient>();

    private static final Map<String, String> SASL_OPTIONS = Collections.singletonMap("SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
    private static final String TEST = "test.war";
    private static final String REPLACEMENT = "test.war.v2";
    private static final String SLAVE = "slave";
    private static final String SECURITY_DOMAIN = "subsystem=security/security-domain=other";
    private static final String HTTP_BINDING = "socket-binding-group=standard-sockets/socket-binding=http";
    private static final String MEMORY_MBEAN = "core-service=platform-mbean/type=memory";
    private static final String DEFAULT_PROFILE = "profile=default";
    private static final String EXAMPLE_DS = "subsystem=datasources/data-source=ExampleDS";
    protected static DomainTestSupport testSupport;
    protected static JBossAsManagedConfiguration masterClientConfig;

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
                                                  JBossAsManagedConfiguration clientConfig) throws UnknownHostException {
        Map<String, ModelControllerClient> clients = allowLocalAuth ? localAuthClients : nonLocalAuthclients;
        ModelControllerClient result = clients.get(userName);
        if (result == null) {
            result = createClient(userName, allowLocalAuth, clientConfig);
            clients.put(userName, result);
        }
        return result;
    }

    private ModelControllerClient createClient(String userName, boolean allowLocalAuth,
                                               JBossAsManagedConfiguration clientConfig) throws UnknownHostException {

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
        String sdAddress = host == null ? DEFAULT_PROFILE + "/" + SECURITY_DOMAIN : SECURITY_DOMAIN;
        readResource(client, sdAddress, host, server, expectedOutcome, roles);
    }

    protected void checkSensitiveAttribute(ModelControllerClient client, String host, String server, boolean expectSuccess, String... roles) throws IOException {
        String dsAddress = host == null ? DEFAULT_PROFILE + "/" + EXAMPLE_DS
                : "host=" + host + "server=" + server + "/" + EXAMPLE_DS;
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
