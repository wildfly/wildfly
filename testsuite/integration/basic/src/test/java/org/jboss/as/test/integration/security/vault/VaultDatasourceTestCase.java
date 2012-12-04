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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.security.vault.VaultSession;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
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

    static class VaultDatasourceTestCaseSetup implements ServerSetupTask {

        private ModelNode originalVault;
        private Connection connection;
        private Server server;
        private VaultSession nonInteractiveSession;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            ModelNode op;

            // setup DB
            server = Server.createTcpServer("-tcpAllowOthers").start();
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", RIGHT_PASSWORD);
            executeUpdate(connection, "CREATE TABLE TestPeople(Name Varchar(50), Surname Varchar(50))");
            executeUpdate(connection, "INSERT INTO TestPeople VALUES ('John','Smith')");

            // copy keystore to temporary file
            FileUtils.copyURLToFile(VaultDatasourceTestCase.class.getResource(KEYSTORE_FILENAME), keyStoreFile);

            // clean temporary directory
            File datFile1 = new File(System.getProperty("java.io.tmpdir"), ENC_DAT_FILE);
            if (datFile1.exists())
                datFile1.delete();
            File datFile2 = new File(System.getProperty("java.io.tmpdir"), SHARED_DAT_FILE);
            if (datFile2.exists())
                datFile2.delete();

            // save original vault setting
            op = new ModelNode();
            op.get(OP).set(READ_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
            op.get(NAME).set(VAULT_OPTIONS);
            originalVault = (managementClient.getControllerClient().execute(new OperationBuilder(op).build())).get(RESULT);

            // remove original vault
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            // create new vault
            String keystoreURL = keyStoreFile.getAbsolutePath();
            String keystorePassword = "password";
            String encryptionDirectory = System.getProperty("java.io.tmpdir") + File.separator;
            String salt = "87654321";
            int iterationCount = 20;

            nonInteractiveSession = new VaultSession(keystoreURL, keystorePassword, encryptionDirectory, salt, iterationCount);
            String vaultAlias = "vault";
            nonInteractiveSession.startVaultSession(vaultAlias);

            // create security attributes
            String attributeName = "password";
            String vaultPasswordString = nonInteractiveSession.addSecuredAttribute(VAULT_BLOCK, attributeName,
                    RIGHT_PASSWORD.toCharArray());
            String wrongVaultPasswordString = nonInteractiveSession.addSecuredAttribute(VAULT_BLOCK_WRONG, attributeName,
                    WRONG_PASSWORD.toCharArray());

            // create new vault setting in standalone
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
            ModelNode vaultOption = op.get(VAULT_OPTIONS);
            vaultOption.get("KEYSTORE_URL").set(keystoreURL);
            vaultOption.get("KEYSTORE_PASSWORD").set(nonInteractiveSession.getKeystoreMaskedPassword());
            vaultOption.get("KEYSTORE_ALIAS").set(vaultAlias);
            vaultOption.get("SALT").set(salt);
            vaultOption.get("ITERATION_COUNT").set(Integer.toString(iterationCount));
            vaultOption.get("ENC_FILE_DIR").set(encryptionDirectory);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

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
            op.get("connection-url").set("jdbc:h2:tcp://" + Utils.getSecondaryTestAddress(managementClient) + "/mem:test");
            op.get("user-name").set("sa");
            op.get("password").set("${" + vaultPasswordString + "}");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            final ModelNode enableNode = new ModelNode();
            enableNode.get(OP).set(ENABLE);
            enableNode.get(OP_ADDR).add(SUBSYSTEM, "datasources");
            enableNode.get(OP_ADDR).add("data-source", VAULT_BLOCK);
            managementClient.getControllerClient().execute(new OperationBuilder(enableNode).build());

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
            op.get("connection-url").set("jdbc:h2:tcp://" + Utils.getSecondaryTestAddress(managementClient) + "/mem:test");
            op.get("user-name").set("sa");
            op.get("password").set("${" + wrongVaultPasswordString + "}");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            final ModelNode enableNodeWrong = new ModelNode();
            enableNodeWrong.get(OP).set(ENABLE);
            enableNodeWrong.get(OP_ADDR).add(SUBSYSTEM, "datasources");
            enableNodeWrong.get(OP_ADDR).add("data-source", VAULT_BLOCK_WRONG);
            managementClient.getControllerClient().execute(new OperationBuilder(enableNodeWrong).build());

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            ModelNode op;

            // remove created datasources
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "datasources");
            op.get(OP_ADDR).add("data-source", VAULT_BLOCK);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "datasources");
            op.get(OP_ADDR).add("data-source", VAULT_BLOCK_WRONG);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            // remove created vault
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            // set original vault
            if (originalVault.get("KEYSTORE_URL") != null) {
                op = new ModelNode();
                op.get(OP).set(ADD);
                op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
                ModelNode vaultOption = op.get(VAULT_OPTIONS);
                vaultOption.get("KEYSTORE_URL").set(originalVault.get("KEYSTORE_URL"));
                vaultOption.get("KEYSTORE_PASSWORD").set(originalVault.get("KEYSTORE_PASSWORD"));
                vaultOption.get("KEYSTORE_ALIAS").set(originalVault.get("KEYSTORE_ALIAS"));
                vaultOption.get("SALT").set(originalVault.get("SALT"));
                vaultOption.get("ITERATION_COUNT").set(originalVault.get("ITERATION_COUNT"));
                vaultOption.get("ENC_FILE_DIR").set(originalVault.get("ENC_FILE_DIR"));
                managementClient.getControllerClient().execute(new OperationBuilder(op).build());
            }

            // remove temporary files
            if (keyStoreFile.exists())
                keyStoreFile.delete();
            File datFile1 = new File(System.getProperty("java.io.tmpdir"), ENC_DAT_FILE);
            if (datFile1.exists())
                datFile1.delete();
            File datFile2 = new File(System.getProperty("java.io.tmpdir"), SHARED_DAT_FILE);
            if (datFile2.exists())
                datFile2.delete();

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

    static final String KEYSTORE_FILENAME = "vaulttest.keystore";
    static final String VAULT_BLOCK = "ds_TestDS";
    static final String VAULT_BLOCK_WRONG = VAULT_BLOCK + "Wrong";
    static final String RIGHT_PASSWORD = "passwordForVault";
    static final String WRONG_PASSWORD = "wrongPasswordForVault";
    static final String ENC_DAT_FILE = "ENC.dat";
    static final String SHARED_DAT_FILE = "Shared.dat";
    static final File keyStoreFile = new File(System.getProperty("java.io.tmpdir"), KEYSTORE_FILENAME);

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
        return war;
    }

}
