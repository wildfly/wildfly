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

package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADMIN_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.domain.management.util.Authentication;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.security.common.VaultHandler;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test a slave HC connecting to the domain using all 3 valid ways of configuring the slave HC's credential:
 * Base64 encoded password, system-property-backed expression, and vault expression.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SlaveHostControllerAuthenticationTestCase {

    private static final String VAULT_BLOCK = "ds_TestDS";
    private static final String RIGHT_PASSWORD = DomainLifecycleUtil.SLAVE_HOST_PASSWORD;
    private static final int TIMEOUT = 60000;

    private static ModelControllerClient domainMasterClient;
    private static ModelControllerClient domainSlaveClient;
    private static DomainTestSupport testSupport;

    static final String RESOURCE_LOCATION = SlaveHostControllerAuthenticationTestCase.class.getProtectionDomain().getCodeSource().getLocation().getFile()
            + "vault-shcatc/";

    @BeforeClass
    public static void setupDomain() throws Exception {

        // Set up a domain with a master that doesn't support local auth so slaves have to use configured credentials
        testSupport = DomainTestSupport.create(
                DomainTestSupport.Configuration.create(SlaveHostControllerAuthenticationTestCase.class.getSimpleName(),
                        "domain-configs/domain-standard.xml",
                        "host-configs/host-master-no-local.xml", "host-configs/host-secrets.xml"));

        // Tweak the callback handler so the master test driver client can authenticate
        // To keep setup simple it uses the same credentials as the slave host
        WildFlyManagedConfiguration masterConfig = testSupport.getDomainMasterConfiguration();
        CallbackHandler callbackHandler = Authentication.getCallbackHandler("slave", RIGHT_PASSWORD, "ManagementRealm");
        masterConfig.setCallbackHandler(callbackHandler);

        testSupport.start();

        domainMasterClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        domainSlaveClient = testSupport.getDomainSlaveLifecycleUtil().getDomainClient();

    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport.stop();
        testSupport = null;
        domainMasterClient = null;
        domainSlaveClient = null;
    }

    @Test
    public void testSlaveRegistration() throws Exception {
        slaveWithBase64PasswordTest();
        slaveWithSystemPropertyPasswordTest();
        slaveWithVaultPasswordTest();
    }

    private void slaveWithBase64PasswordTest() throws Exception {
        // Simply check that the initial startup produced a registered slave
        readHostControllerStatus(domainMasterClient, 0);
    }

    private void slaveWithSystemPropertyPasswordTest() throws Exception {

        // Set the slave secret to a system-property-backed expression
        setSlaveSecret("${slave.secret:" + RIGHT_PASSWORD + "}");

        reloadSlave();

        // Validate that it joined the master
        readHostControllerStatus(domainMasterClient, 0);
    }

    private void slaveWithVaultPasswordTest() throws Exception {

        VaultHandler.cleanFilesystem(RESOURCE_LOCATION, true);

        // create new vault
        VaultHandler vaultHandler = new VaultHandler(RESOURCE_LOCATION);

        try {

            // create security attributes
            String attributeName = "value";
            String vaultPasswordString = vaultHandler.addSecuredAttribute(VAULT_BLOCK, attributeName,
                    RIGHT_PASSWORD.toCharArray());

            // create new vault setting in host
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(HOST, "slave").add(CORE_SERVICE, VAULT);
            ModelNode vaultOption = op.get(VAULT_OPTIONS);
            vaultOption.get("KEYSTORE_URL").set(vaultHandler.getKeyStore());
            vaultOption.get("KEYSTORE_PASSWORD").set(vaultHandler.getMaskedKeyStorePassword());
            vaultOption.get("KEYSTORE_ALIAS").set(vaultHandler.getAlias());
            vaultOption.get("SALT").set(vaultHandler.getSalt());
            vaultOption.get("ITERATION_COUNT").set(vaultHandler.getIterationCountAsString());
            vaultOption.get("ENC_FILE_DIR").set(vaultHandler.getEncodedVaultFileDirectory());
            domainSlaveClient.execute(new OperationBuilder(op).build());

            setSlaveSecret("${" + vaultPasswordString + "}");

            reloadSlave();

            // Validate that it joined the master
            readHostControllerStatus(domainMasterClient, 0);
        } finally {
            // remove temporary files
            vaultHandler.cleanUp();
        }
    }

    private static void reloadSlave() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set("reload");
        op.get(OP_ADDR).add(HOST, "slave");
        op.get(ADMIN_ONLY).set(false);
        try {
            domainSlaveClient.execute(new OperationBuilder(op).build());
        } catch(IOException e) {
            final Throwable cause = e.getCause();
            if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                throw e;
            } // else ignore, this might happen if the channel gets closed before we got the response
        }

        // Wait until host is reloaded
        readHostControllerStatus(domainSlaveClient, TIMEOUT);
    }

    private static void readHostControllerStatus(ModelControllerClient client, long timeout) throws Exception {
        final long time = System.currentTimeMillis() + timeout;
        do {
            Thread.sleep(250);
            if (lookupHostInModel(client)) {
                return;
            }
        } while (System.currentTimeMillis() < time);

        Assert.fail("Cannot validate host 'slave' is running");
    }

    private static boolean lookupHostInModel(ModelControllerClient client) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(HOST, "slave");
        operation.get(NAME).set(HOST_STATE);

        try {
            final ModelNode result = client.execute(operation);
            if (result.get(OUTCOME).asString().equals(SUCCESS)){
                final ModelNode model = result.require(RESULT);
                if (model.asString().equalsIgnoreCase("running")) {
                    return true;
                }
            }
        } catch (IOException e) {
            //
        }
        return false;
    }

    private static void setSlaveSecret(String value) throws IOException {

        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(HOST, "slave").add(CORE_SERVICE, MANAGEMENT).add(SECURITY_REALM, "ManagementRealm").add(SERVER_IDENTITY, SECRET);
        op.get(NAME).set(VALUE);
        op.get(VALUE).set(value);
        domainSlaveClient.execute(new OperationBuilder(op).build());

    }
}
