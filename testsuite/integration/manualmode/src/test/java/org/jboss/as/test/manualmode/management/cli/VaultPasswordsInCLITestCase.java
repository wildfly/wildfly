/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.management.cli;

import static org.hamcrest.CoreMatchers.containsString;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.vault.VaultSession;
import org.jboss.as.test.integration.management.util.CustomCLIExecutor;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing support for vault encrypted passwords in jboss-cli configuration
 * file. It tries to connect to native management interface with cli console
 * with configured SSL.
 * 
 * @author Filip Bogyai
 */

@RunWith(Arquillian.class)
@ServerSetup({ VaultPasswordsInCLITestCase.ManagemenCLIRealmSetup.class, VaultPasswordsInCLITestCase.ManagementInterfacesSetup.class })
@RunAsClient
public class VaultPasswordsInCLITestCase {

    private static Logger LOGGER = Logger.getLogger(VaultPasswordsInCLITestCase.class);

    private static final File WORK_DIR = new File("native-if-workdir");
    public static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    public static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    public static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    public static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);

    // see genseckey-cli-vault-keystore in pom.xml for the keystore creation
    public static final File VAULT_KEYSTORE_FILE = new File("../cli-vault.keystore");
    public static final String VAULT_KEYSTORE_PASS = "secret_1";
    public static final String ALIAS_NAME = "vault";

    private static VaultSession nonInteractiveSession;

    private static final String MANAGEMENT_CLI_REALM = "ManagementCLIRealm";
    private static final String JBOSS_CLI_FILE = "jboss-cli.xml";
    private static final File TRUSTED_JBOSS_CLI_FILE = new File(WORK_DIR, "trusted-jboss-cli.xml");
    private static final File RIGHT_VAULT_PASSWORD_FILE = new File(WORK_DIR, "right-vault-pass.xml");
    private static final File WRONG_VAULT_PASSWORD_FILE = new File(WORK_DIR, "wrong-vault-pass.xml");

    private static final String TESTING_OPERATION = "/core-service=management/management-interface=http-interface:read-resource";
    private static final String RELOAD = "reload";
    private static final int MAX_RELOAD_TIME = 30000;

    private static final String CONTAINER = "default-jbossas";
    private static final ManagementInterfacesSetup managementInterfacesSetup = new ManagementInterfacesSetup();
    private static final ManagemenCLIRealmSetup managementCLIRealmSetup = new ManagemenCLIRealmSetup();

    @ArquillianResource
    private static ContainerController containerController;

    @Test
    @InSequence(-1)
    public void prepareServer() throws Exception {

        LOGGER.info("*** starting server");
        containerController.start(CONTAINER);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient mgmtClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());
        managementInterfacesSetup.setup(mgmtClient, CONTAINER);
        managementCLIRealmSetup.setup(mgmtClient, CONTAINER);
        mgmtClient.close();

        createAndInitializeVault();

        // To apply new security realm settings for native interface reload of
        // server is required
        LOGGER.info("*** restart server");
        CustomCLIExecutor.execute(TRUSTED_JBOSS_CLI_FILE, RELOAD);
        CustomCLIExecutor.waitForServerToReload(MAX_RELOAD_TIME, TRUSTED_JBOSS_CLI_FILE);

    }

    /**
     * Testing access to native interface with wrong password to keystore in
     * jboss-cli configuration. This password is masked and is loaded from
     * vault. Exception with message that password is incorrect should be
     * thrown.
     */
    @Test
    @InSequence(1)
    public void testWrongVaultPassword() throws InterruptedException, IOException {

        String cliOutput = CustomCLIExecutor.execute(WRONG_VAULT_PASSWORD_FILE, TESTING_OPERATION);
        assertThat("Password should be wrong", cliOutput, containsString("CliInitializationException"));

    }

    /**
     * Testing access to native interface with right password to keystore in
     * jboss-cli configuration. This password is masked and is loaded from
     * vault.
     */
    @Test
    @InSequence(2)
    public void testRightVaultPassword() throws InterruptedException, IOException {

        String cliOutput = CustomCLIExecutor.execute(RIGHT_VAULT_PASSWORD_FILE, TESTING_OPERATION);
        assertThat("Password should be right and authentication successful", cliOutput, containsString("\"outcome\" => \"success\""));

    }

    @Test
    @InSequence(3)
    public void resetConfigurationForNativeInterface() throws Exception {

        LOGGER.info("*** reseting test configuration");
        // change back security realm for native management interface
        String unsecureNativeInterface = "/core-service=management/management-interface=native-interface:write-attribute(name=security-realm,value=ManagementRealm";
        String cliOutput = CustomCLIExecutor.execute(TRUSTED_JBOSS_CLI_FILE, unsecureNativeInterface);
        assertThat("Revert of native-interface security realm was unsuccessful", cliOutput, containsString("\"outcome\" => \"success\""));

        // reload to apply changes
        CustomCLIExecutor.execute(TRUSTED_JBOSS_CLI_FILE, RELOAD);
        CustomCLIExecutor.waitForServerToReload(MAX_RELOAD_TIME, TRUSTED_JBOSS_CLI_FILE);

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient mgmtClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());
        managementInterfacesSetup.tearDown(mgmtClient, CONTAINER);
        managementCLIRealmSetup.tearDown(mgmtClient, CONTAINER);
        mgmtClient.close();

        LOGGER.info("*** stopping container");
        containerController.stop(CONTAINER);

    }

    static class ManagemenCLIRealmSetup extends AbstractSecurityRealmsServerSetupTask {

        @Override
        protected SecurityRealm[] getSecurityRealms() throws Exception {
            final ServerIdentity serverIdentity = new ServerIdentity.Builder().ssl(
                    new RealmKeystore.Builder().keystorePassword(SecurityTestConstants.KEYSTORE_PASSWORD)
                            .keystorePath(SERVER_KEYSTORE_FILE.getAbsolutePath()).build()).build();
            final Authentication authentication = new Authentication.Builder().truststore(
                    new RealmKeystore.Builder().keystorePassword(SecurityTestConstants.KEYSTORE_PASSWORD)
                            .keystorePath(SERVER_TRUSTSTORE_FILE.getAbsolutePath()).build()).build();
            final SecurityRealm realm = new SecurityRealm.Builder().name(MANAGEMENT_CLI_REALM).serverIdentity(serverIdentity)
                    .authentication(authentication).build();
            return new SecurityRealm[] { realm };
        }
    }

    static class ManagementInterfacesSetup implements ServerSetupTask {

        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            FileUtils.deleteDirectory(WORK_DIR);
            WORK_DIR.mkdirs();
            Utils.createKeyMaterial(WORK_DIR);
            FileUtils.write(TRUSTED_JBOSS_CLI_FILE, Utils.propertiesReplacer(JBOSS_CLI_FILE, CLIENT_KEYSTORE_FILE, CLIENT_TRUSTSTORE_FILE,
                    SecurityTestConstants.KEYSTORE_PASSWORD));

            final ModelControllerClient client = managementClient.getControllerClient();

            // secure native interface
            ModelNode operation = createOpNode("core-service=management/management-interface=native-interface",
                    ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("security-realm");
            operation.get(VALUE).set(MANAGEMENT_CLI_REALM);
            Utils.applyUpdate(operation, client);

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            FileUtils.deleteDirectory(WORK_DIR);

        }
    }
    
    private void createAndInitializeVault() throws Exception {

        // see genseckey-cli-vault-keystore in pom.xml for the keystore creation
        String keystoreURL = VAULT_KEYSTORE_FILE.getAbsolutePath();
        String keystoreDirURL = WORK_DIR.getAbsolutePath();
        String salt = "87654321";
        int iterationCount = 20;
        nonInteractiveSession = new VaultSession(keystoreURL, VAULT_KEYSTORE_PASS, keystoreDirURL, salt, iterationCount);
        nonInteractiveSession.startVaultSession(ALIAS_NAME);

        String rightPassword = SecurityTestConstants.KEYSTORE_PASSWORD;
        String wrongPassword = "blablabla";
        String rightBlock = "right";
        String wrongBlock = "wrong";
        String attributeName = "password";

        // add right and wrong password for clients keystore into vault
        String vaultPasswordString = nonInteractiveSession.addSecuredAttribute(rightBlock, attributeName, rightPassword.toCharArray());
        String wrongVaultPasswordString = nonInteractiveSession.addSecuredAttribute(wrongBlock, attributeName, wrongPassword.toCharArray());

        String vaultConfiguration = nonInteractiveSession.vaultConfiguration();

        // create jboss-cli configuration file with ssl and vaulted passwords
        FileUtils.write(RIGHT_VAULT_PASSWORD_FILE, Utils.propertiesReplacer(JBOSS_CLI_FILE, CLIENT_KEYSTORE_FILE, CLIENT_TRUSTSTORE_FILE,
                vaultPasswordString, vaultConfiguration));
        FileUtils.write(WRONG_VAULT_PASSWORD_FILE, Utils.propertiesReplacer(JBOSS_CLI_FILE, CLIENT_KEYSTORE_FILE, CLIENT_TRUSTSTORE_FILE,
                wrongVaultPasswordString, vaultConfiguration));

    }

}