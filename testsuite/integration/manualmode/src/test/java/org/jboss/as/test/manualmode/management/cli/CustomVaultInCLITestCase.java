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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.util.CustomCLIExecutor;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing support of custom security vaults for encrypting passwords in
 * jboss-cli configuration file. It tries to invoke CLI client with vaulted
 * passwords for truststore in its configuration, which results in resolving
 * vault expression.
 * 
 * @author Filip Bogyai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CustomVaultInCLITestCase {

    private static Logger LOGGER = Logger.getLogger(CustomVaultInCLITestCase.class);
    
    private static final File WORK_DIR = new File("cli-custom-vault-workdir");

    public static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    public static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    private static final String RIGHT_PASSWORD = SecurityTestConstants.KEYSTORE_PASSWORD;
    private static final String WRONG_PASSWORD = "someRandomWrongPass";

    private static final File MODULE_FILE = new File(WORK_DIR, "module.xml");

    private static final String JBOSS_CLI_FILE = "jboss-cli.xml";
    private static final File RIGHT_VAULT_PASSWORD_FILE = new File(WORK_DIR, "right-vault-pass.xml");
    private static final File WRONG_VAULT_PASSWORD_FILE = new File(WORK_DIR, "wrong-vault-pass.xml");
    private static final File NON_EXISTING_VAULT_PASSWORD_FILE = new File(WORK_DIR, "non-existing-vault-pass.xml");


    private static String MODULE_NAME = "test.custom.vault.in.cli";
    private static String JAR_NAME = "custom-dummy-vault-module.jar";
    private static final String CONTAINER = "default-jbossas";
    private static TestModule customVaultModule;

    @ArquillianResource
    private static ContainerController containerController;

    @Test
    @InSequence(-1)
    public void prepareConfiguration() throws Exception {

        FileUtils.deleteDirectory(WORK_DIR);
        WORK_DIR.mkdirs();
        Utils.createKeyMaterial(WORK_DIR);

        createCustomVaultModule();
        createCustomVaultConfiguration();

        LOGGER.info("*** starting server");
        containerController.start(CONTAINER);
    }

    /**
     * Run CLI with vaulted keystore and truststore passwords. Vault expression
     * should return right password.
     */
    @Test
    @InSequence(1)
    public void testRightVaultPassword() throws Exception {

        String cliOutput = CustomCLIExecutor.execute(RIGHT_VAULT_PASSWORD_FILE, 
                READ_ATTRIBUTE_OPERATION + " server-state");

        assertThat("Password should be right", cliOutput, containsString("Password is: " + RIGHT_PASSWORD));
        assertThat("CLI should successfully initialize ", cliOutput, containsString("running"));

    }

    /**
     * Run CLI with vaulted keystore and truststore passwords. Vault expression
     * should return wrong password, so CliInitializationException is expected
     */
    @Test
    @InSequence(2)
    public void testWrongVaultPassword() throws Exception {

        String cliOutput = CustomCLIExecutor.execute(WRONG_VAULT_PASSWORD_FILE, 
                READ_ATTRIBUTE_OPERATION + " server-state");

        assertThat("Password should be wrong", cliOutput, containsString("Password is: " + WRONG_PASSWORD));
        assertThat("CLI shouldn't successfully initialize ", cliOutput, containsString("CliInitializationException"));

    }

    /**
     * Run CLI with vaulted keystore and truststore passwords. Vault expression
     * should not exists, so NullPointerException is expected
     */
    @Test
    @InSequence(3)
    public void testNonExistingVaultPassword() throws Exception {

        String cliOutput = CustomCLIExecutor.execute(NON_EXISTING_VAULT_PASSWORD_FILE, 
                READ_ATTRIBUTE_OPERATION + " server-state");

        assertThat("Password should not exists", cliOutput, containsString("NullPointerException"));

    }

    @Test
    @InSequence(4)
    public void rollbackConfiguration() throws Exception {

        LOGGER.info("*** stopping server");
        containerController.stop(CONTAINER);

        customVaultModule.remove();

    }

    private void createCustomVaultModule() throws IOException {

        String moduleXML = "<module xmlns=\"urn:jboss:module:1.1\" name=\""+ MODULE_NAME +"\">" + 
                               "<resources> <resource-root path=\""+ JAR_NAME +"\"/>  </resources>" + 
                               "<dependencies> <module name=\"org.picketbox\"/> </dependencies> "+
                           "</module>";

        FileUtils.write(MODULE_FILE, moduleXML);

        customVaultModule = new TestModule(MODULE_NAME, MODULE_FILE);
        customVaultModule.addResource(JAR_NAME).addClass(CustomDummyVault.class);
        customVaultModule.create(true);
        
    }
    
    private void createCustomVaultConfiguration() throws IOException{
        
        String rightBlock = "rightVaultBlock";
        String wrongBlock = "wrongVaultBlock";
        
        String vaultConfig = "<vault code=\""+ CustomDummyVault.class.getName() + "\" module=\"" + MODULE_NAME+ "\">" +
                "<vault-option name=\""+ rightBlock +"\" value=\"" + RIGHT_PASSWORD + "\"/>" +
                "<vault-option name=\""+ wrongBlock +"\" value=\"" + WRONG_PASSWORD + "\"/>" +       
            "</vault>";

        //passwords are defined above and are retrieved depending only on vault block
        String vaultPasswordString = "VAULT::" + rightBlock + "::good::1";
        String wrongVaultPasswordString = "VAULT::" + wrongBlock + "::wrong::1";
        String nonExistingVaultPasswordString = "VAULT::nonExistingBlock::non::1";
        
        // create jboss-cli configuration file with vaulted passwords and change xsd to 3.0
        String rightVaultPassConfig = Utils.propertiesReplacer(JBOSS_CLI_FILE, CLIENT_KEYSTORE_FILE, CLIENT_TRUSTSTORE_FILE,
                vaultPasswordString, vaultConfig).replaceAll("urn:jboss:cli:2.0", "urn:jboss:cli:3.0");
        String wrongVaultPassConfig = Utils.propertiesReplacer(JBOSS_CLI_FILE, CLIENT_KEYSTORE_FILE, CLIENT_TRUSTSTORE_FILE,
                wrongVaultPasswordString, vaultConfig).replaceAll("urn:jboss:cli:2.0", "urn:jboss:cli:3.0");
        String nonVaultPassConfig = Utils.propertiesReplacer(JBOSS_CLI_FILE, CLIENT_KEYSTORE_FILE, CLIENT_TRUSTSTORE_FILE,
                nonExistingVaultPasswordString, vaultConfig).replaceAll("urn:jboss:cli:2.0", "urn:jboss:cli:3.0");
        
        FileUtils.write(RIGHT_VAULT_PASSWORD_FILE, rightVaultPassConfig);
        FileUtils.write(WRONG_VAULT_PASSWORD_FILE, wrongVaultPassConfig);
        FileUtils.write(NON_EXISTING_VAULT_PASSWORD_FILE, nonVaultPassConfig);
        
    }

}
