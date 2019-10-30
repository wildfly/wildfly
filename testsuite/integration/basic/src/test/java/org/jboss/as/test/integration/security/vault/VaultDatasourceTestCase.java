/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.vault;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.SocketPermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.VaultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A VaultDatasourceTestCase for testing access to database through vault security
 *
 * @author Ondrej Lukas
 */
@RunWith(Arquillian.class)
@ServerSetup(VaultDatasourceTestCase.VaultDatasourceTestCaseSetup.class)
public class VaultDatasourceTestCase {

    private static Logger LOGGER = Logger.getLogger(VaultDatasourceTestCase.class);

    static class VaultDatasourceTestCaseSetup implements ServerSetupTask {

        private Connection connection;
        private Server server;
        private VaultHandler vaultHandler;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            LOGGER.trace("RESOURCE_LOCATION=" + RESOURCE_LOCATION);

            VaultHandler.cleanFilesystem(RESOURCE_LOCATION, true);

            ModelNode op;

            // setup DB
            server = Server.createTcpServer("-tcpAllowOthers").start();

            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection("jdbc:h2:mem:" + VaultDatasourceTestCase.class.getName() +";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "sa", RIGHT_PASSWORD);
            executeUpdate(connection, "CREATE TABLE TestPeople(Name Varchar(50), Surname Varchar(50))");
            executeUpdate(connection, "INSERT INTO TestPeople VALUES ('John','Smith')");
            // create new vault
            vaultHandler = new VaultHandler(RESOURCE_LOCATION);

            // create security attributes
            String attributeName = "password";
            String vaultPasswordString = vaultHandler.addSecuredAttribute(VAULT_BLOCK, attributeName,
                    RIGHT_PASSWORD.toCharArray());
            String wrongVaultPasswordString = vaultHandler.addSecuredAttribute(VAULT_BLOCK_WRONG, attributeName,
                    WRONG_PASSWORD.toCharArray());

            LOGGER.debug("vaultPasswordString=" + vaultPasswordString);

            // create new vault setting in standalone
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
            ModelNode vaultOption = op.get(VAULT_OPTIONS);
            vaultOption.get("KEYSTORE_URL").set(vaultHandler.getKeyStore());
            vaultOption.get("KEYSTORE_PASSWORD").set(vaultHandler.getMaskedKeyStorePassword());
            vaultOption.get("KEYSTORE_ALIAS").set(vaultHandler.getAlias());
            vaultOption.get("SALT").set(vaultHandler.getSalt());
            vaultOption.get("ITERATION_COUNT").set(vaultHandler.getIterationCountAsString());
            vaultOption.get("ENC_FILE_DIR").set(vaultHandler.getEncodedVaultFileDirectory());
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            LOGGER.debug("Vault created in sever configuration");

            // create new datasource with right password
            ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, "datasources");
            address.add("data-source", VAULT_BLOCK);
            address.protect();
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).set(address);
            op.get("jndi-name").set("java:jboss/datasources/" + VAULT_BLOCK);
            op.get("driver-name").set("h2");
            op.get("connection-url").set("jdbc:h2:tcp://" + Utils.getSecondaryTestAddress(managementClient) + "/mem:" + VaultDatasourceTestCase.class.getName());
            op.get("user-name").set("sa");
            op.get("password").set("${" + vaultPasswordString + "}");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            LOGGER.debug(VAULT_BLOCK + " datasource created");

            // create new datasource with wrong password
            address = new ModelNode();
            address.add(SUBSYSTEM, "datasources");
            address.add("data-source", VAULT_BLOCK_WRONG);
            address.protect();
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).set(address);
            op.get("jndi-name").set("java:jboss/datasources/" + VAULT_BLOCK_WRONG);
            op.get("driver-name").set("h2");
            op.get("connection-url").set("jdbc:h2:tcp://" + Utils.getSecondaryTestAddress(managementClient) + "/mem:" + VaultDatasourceTestCase.class.getName());
            op.get("user-name").set("sa");
            op.get("password").set("${" + wrongVaultPasswordString + "}");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            LOGGER.debug(VAULT_BLOCK_WRONG + " datasource created");

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            ModelNode op;

            // remove created datasources
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "datasources");
            op.get(OP_ADDR).add("data-source", VAULT_BLOCK);
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "datasources");
            op.get(OP_ADDR).add("data-source", VAULT_BLOCK_WRONG);
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            // remove created vault
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            // remove temporary files
            vaultHandler.cleanUp();

            // stop DB
            executeUpdate(connection, "DROP TABLE TestPeople");
            connection.close();
            server.shutdown();

        }

        private void executeUpdate(Connection connection, String query) throws SQLException {
            final Statement statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
        }
    }

    static final String RESOURCE_LOCATION = VaultDatasourceTestCase.class.getResource("/").getPath()
            + "security/ds-vault/";
    static final String VAULT_BLOCK = "ds_TestDS";
    static final String VAULT_BLOCK_WRONG = VAULT_BLOCK + "Wrong";
    static final String RIGHT_PASSWORD = "PasswordForVault";
    static final String WRONG_PASSWORD = "wrongPasswordForVault";

    /*
     * Tests that can access to database with right password
     */
    @Test
    public void testAccessThroughVaultDatasource() throws Exception {
        Context initialContext = new InitialContext();
        DataSource ds = (DataSource) initialContext.lookup("java:jboss/datasources/" + VAULT_BLOCK);
        Assert.assertNotNull(ds);
        Connection con = null;
        try {
            con = ds.getConnection();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        ResultSet rs = con.prepareStatement("SELECT Name FROM TestPeople WHERE Surname='Smith'").executeQuery();
        rs.next();
        assertEquals(rs.getString("Name"), "John");
        con.close();
    }

    /*
     * Tests that can't access to database with wrong password
     */
    @Test
    public void testRejectWrongPasswordThroughVaultDatasource() throws Exception {
        Context initialContext = new InitialContext();
        DataSource ds = (DataSource) initialContext.lookup("java:jboss/datasources/" + VAULT_BLOCK_WRONG);
        Assert.assertNotNull(ds);
        Connection con = null;
        try {
            con = ds.getConnection();
            con.close();
            fail("Connect to database with wrong password!");
        } catch (Exception ex) {
        }
    }

    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.addAsManifestResource(createPermissionsXmlAsset(new SocketPermission("*:9092", "connect,resolve")), "permissions.xml");
        return war;
    }

}
