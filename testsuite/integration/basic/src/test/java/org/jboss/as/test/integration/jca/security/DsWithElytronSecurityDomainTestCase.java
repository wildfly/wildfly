/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertNotNull;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Data source with security domain test JBQA-5952
 *
 * @author <a href="mailto:vrastsel@redhat.com"> Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(DsWithElytronSecurityDomainTestCase.DsWithElytronSecurityDomainTestCaseSetup.class)
public class DsWithElytronSecurityDomainTestCase {

    private static final String DATASOURCE_NAME = "ElyDSTest";
    private static final PathAddress DATASOURCES_SUBSYSTEM_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "datasources");

    private static final PathAddress ELYTRON_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, "elytron");
    public static final String FS_REALM = "fsRealm001";
    public static final String AUTH_CTX = "myAuthCtx";
    private static final PathAddress FS_REALM_ADDRESS = ELYTRON_ADDRESS.append("filesystem-realm", "fsRealm001");
    private static final PathAddress AUTH_ADDRESS = ELYTRON_ADDRESS.append("authentication-context", AUTH_CTX);
    private static final String SECURITY_DOMAIN = "ElyDSSecDomain";
    private static final String DATABASE_PASSWORD = "chucknorris";
    private static final PathAddress IDENTITY_ADDR = FS_REALM_ADDRESS.append(IDENTITY, "sa");
    private static final PathAddress ROLE_DECODER_ADDR = ELYTRON_ADDRESS.append("simple-role-decoder", "from-roles-attribute");
    private static final PathAddress SECURITY_DOMAIN_ADDR = ELYTRON_ADDRESS.append("security-domain", SECURITY_DOMAIN);


    public static class DsWithElytronSecurityDomainTestCaseSetup implements ServerSetupTask {

        private Server h2Server;
        private Connection connection;


        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {

            final ModelControllerClient client = managementClient.getControllerClient();
            setupDB();
            createFsRealm(client);
            addUser(client);
            addSimpleRoleDecoder(client);
            addSecurityDomain(client);
            addAuthCtx(client);

            createDS(client);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            final ModelControllerClient client = managementClient.getControllerClient();

            removeDatasourceSilently(managementClient.getControllerClient());
            removeSilently(client, SECURITY_DOMAIN_ADDR);
            removeSilently(client, ROLE_DECODER_ADDR);
            removeSilently(client, IDENTITY_ADDR);
            removeSilently(client, FS_REALM_ADDRESS);
            tearDownDB();
        }

        private void createFsRealm(final ModelControllerClient client) throws IOException {
            final ModelNode addOperation = Operations.createAddOperation(FS_REALM_ADDRESS.toModelNode());
            addOperation.get(PATH).set(FS_REALM);
            addOperation.get(RELATIVE_TO).set("jboss.server.data.dir");
            execute(client, addOperation);
        }

        private void addUser(final ModelControllerClient client) throws IOException {
            final ModelNode addOperation = Operations.createAddOperation(IDENTITY_ADDR.toModelNode());
            execute(client, addOperation);
            final ModelNode setPasswordOperation = Operations.createOperation("set-password", IDENTITY_ADDR.toModelNode());
            ModelNode clear = setPasswordOperation.get("clear").setEmptyObject();
            clear.get("password").set(DATABASE_PASSWORD);
            execute(client, setPasswordOperation);
            final ModelNode addAttribute = Operations.createOperation("add-attribute", IDENTITY_ADDR.toModelNode());
            addAttribute.get(NAME).set("Roles");
            addAttribute.get(VALUE).addEmptyList();
            addAttribute.get(VALUE).set(Arrays.asList(new ModelNode("Admin"), new ModelNode("Guest")));
            execute(client, addAttribute);

        }

        private void addSimpleRoleDecoder(final ModelControllerClient client) throws IOException {
            final ModelNode addOperation = Operations.createAddOperation(ROLE_DECODER_ADDR.toModelNode());
            addOperation.get("attribute").set("Roles");
            execute(client, addOperation);

        }

        private void addSecurityDomain(final ModelControllerClient client) throws IOException {
            final ModelNode addOperation = Operations.createAddOperation(SECURITY_DOMAIN_ADDR.toModelNode());
            ModelNode realms= addOperation.get("realms").addEmptyObject();
            realms.get("realm").set(FS_REALM);
            realms.get("role-decoder").set("from-roles-attribute");
            addOperation.get("default-realm").set(FS_REALM);
            addOperation.get("permission-mapper").set("default-permission-mapper");
            execute(client, addOperation);

        }

        private void addAuthCtx(final ModelControllerClient client) throws IOException {
            final ModelNode addOperation = Operations.createAddOperation(AUTH_ADDRESS.toModelNode());
            ModelNode matchRules= addOperation.get("match-rules").addEmptyObject();
            matchRules.get("match-local-security-domain").set(SECURITY_DOMAIN);
            execute(client, addOperation);

        }

        private ModelNode execute(final ModelControllerClient client, final ModelNode op) throws IOException {
            final ModelNode result = client.execute(op);
            return result;
        }


        private void createDS(final ModelControllerClient client) throws IOException {
            final ModelNode addOperation = Operations.createAddOperation(DATASOURCES_SUBSYSTEM_ADDRESS.append("data-source", DATASOURCE_NAME).toModelNode());
            addOperation.get("jndi-name").set("java:jboss/datasources/" + DATASOURCE_NAME);
            addOperation.get("driver-name").set("h2");
            addOperation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            addOperation.get("elytron-enabled").set(true);
            addOperation.get("authentication-context").set(AUTH_CTX);
            addOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
            execute(client, addOperation);
        }
        private void removeDatasourceSilently(final ModelControllerClient client) throws IOException {
            removeSilently(client, DATASOURCES_SUBSYSTEM_ADDRESS.append("data-source", DATASOURCE_NAME));


        }

        private void removeSilently(final ModelControllerClient client, PathAddress address) throws IOException {
            final ModelNode removeOperation = Operations.createRemoveOperation(address.toModelNode());
            removeOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
            execute(client, removeOperation);

        }

        public void setupDB() throws Exception {
            h2Server = Server.createTcpServer("-tcpAllowOthers").start();
            // open connection to database, because that's only (easy) way to set password for user sa
            connection = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "sa", DATABASE_PASSWORD);
        }

        public void tearDownDB() throws Exception {
            connection.close();
            h2Server.shutdown();
        }
    }

    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.addAsManifestResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller-client, org.jboss.as.controller\n"), "MANIFEST.MF");
        war.addClass(DsWithElytronSecurityDomainTestCase.class);
        war.addClass(DsWithElytronSecurityDomainTestCaseSetup.class);
        return war;
    }

    @ArquillianResource
    private InitialContext ctx;

    @Test
    public void deploymentTest() throws Exception {
        DataSource ds = (DataSource) ctx.lookup("java:jboss/datasources/" + DATASOURCE_NAME);
        Connection con = null;
        try {
            con = ds.getConnection();
            assertNotNull(con);

        } finally {
            if (con != null) { con.close(); }
        }
    }
}
