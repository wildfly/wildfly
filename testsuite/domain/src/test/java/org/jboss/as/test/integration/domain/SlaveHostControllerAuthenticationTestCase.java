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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;

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
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test a slave HC connecting to the domain using all 3 valid ways of configuring the slave HC's credential:
 * Base64 encoded password, system-property-backed expression, and vault expression.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SlaveHostControllerAuthenticationTestCase extends AbstractSlaveHCAuthenticationTestCase {

    private static final String VAULT_BLOCK = "ds_TestDS";
    private static final String RIGHT_PASSWORD = DomainLifecycleUtil.SLAVE_HOST_PASSWORD;

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

    @Override
    protected ModelControllerClient getDomainMasterClient() {
        return domainMasterClient;
    }

    @Override
    protected ModelControllerClient getDomainSlaveClient() {
        return domainSlaveClient;
    }
}
