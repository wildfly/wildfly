/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2014, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.vault.VaultSession;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.VaultHandler;
import org.jboss.as.test.integration.security.loginmodules.ExternalPasswordProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.security.util.StringPropertyReplacer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for Security Vault external passwords, obtained from external command
 * or class. Each test case initialize a test vault with different password
 * type.
 *
 * @author Filip Bogyai
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ExternalPasswordModuleSetupTask.class,
        ExternalPasswordCommandsTestCase.ExternalVaultPasswordSetup.class})
public class ExternalPasswordCommandsTestCase {

    private static Logger LOGGER = Logger.getLogger(ExternalPasswordCommandsTestCase.class);

    @ArquillianResource
    private ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    public static final String ATTRIBUTE_NAME = "someAttributeName";
    public static final String VAULT_BLOCK = "someVaultBlockaj";
    public static final String VAULT_ATTRIBUTE = "secretValue";
    public static final String VAULTED_PROPERTY = "VAULT::" + VAULT_BLOCK + "::" + ATTRIBUTE_NAME + "::1";

    public static String KEYSTORE_URL = "vault.keystore";
    public static final String VAULT_PASSWORD = "VaultPassword";
    public static final String VAULT_ALIAS = "vault";
    public static String RESOURCE_LOCATION = "";
    public static final String VAULT_DAT_FILE = RESOURCE_LOCATION + "VAULT.dat";
    public static final String SALT = "87654321";
    public static final int ITER_COUNT = 47;

    private static final ExternalPasswordProvider passwordProvider = new ExternalPasswordProvider(System.getProperty("java.io.tmpdir")
            + File.separator + "tmp.counter");
    public static final PathAddress VAULT_PATH = PathAddress.pathAddress().append(CORE_SERVICE, VAULT);

    /**
     * Test deployment used for checking if vaulted password can be loaded from
     * vault configured in server config.
     */
    @Deployment(name = "vault")
    public static WebArchive appDeploymentCahce() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "vault" + ".war");
        war.addClass(CheckVaultedPassServlet.class);
        war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.picketbox"), "jboss-deployment-structure.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("org.jboss.security.vault.SecurityVaultFactory.get")), "permissions.xml");
        return war;
    }

    /**
     * Testing {EXT} option password as a exact command to execute. The exact
     * command is a string delimited by ' '.
     */
    @Test
    public void testExtPassword() throws Exception {

        createVault(buildCommand("EXT", " "));

        Assert.assertEquals(VAULT_ATTRIBUTE, getVaultedPassword());

        removeVault();
    }

    /**
     * Testing {CMD} option password as a general command to execute. The
     * general command is a string delimited by ',' where the first part is the
     * actual command and further parts represents its parameters.
     */
    @Test
    public void testCmdPassword() throws Exception {

        createVault(buildCommand("CMD", ","));

        Assert.assertEquals(VAULT_ATTRIBUTE, getVaultedPassword());

        removeVault();
    }

    /**
     * Testing {CLASS[@module_name]}classname' option of password where the
     * the class @ExternalPassword is loaded from custom module and toCharArray()
     * method return password.
     */
    @Test
    public void testCustomModuleClassPassword() throws Exception {

        createVault("{CLASS@" + ExternalPasswordModuleSetupTask.getModuleName() + "}" + ExternalPassword.class.getName());

        Assert.assertEquals(VAULT_ATTRIBUTE, getVaultedPassword());

        removeVault();

    }

    /**
     * Testing {CLASS[@module_name]}classname[:ctorargs]' option of password
     * where the the class @ExternalPassword is loaded from custom module and
     * toCharArray() method return password. The class constructor takes two
     * arguments, which will be used to construct the password.
     */
    @Test
    public void testCustomModuleClassWithArguments() throws Exception {

        createVault("{CLASS@" + ExternalPasswordModuleSetupTask.getModuleName() + "}"
                + ExternalPassword.class.getName() + ":Vault,Password");

        Assert.assertEquals(VAULT_ATTRIBUTE, getVaultedPassword());

        removeVault();

    }


    /**
     * TestingTesting {CLASS[@module_name]}classname[:ctorargs]' option password
     * where the '[:ctorargs]' is an optional string delimited by the ':' from
     * the classname that will be passed to the class constructor. The class
     *
     * @TmpFilePassword constructor takes one argument with file, in which the password
     * is stored password.
     */
    @Test
    public void testPicketboxClassPassword() throws Exception {

        File tmpPassword = new File(System.getProperty("java.io.tmpdir"), "tmp.password");
        FileWriter writer = new FileWriter(tmpPassword);
        writer.write(VAULT_PASSWORD);
        writer.close();

        String passwordCmd = "{CLASS@org.picketbox}org.jboss.security.plugins.TmpFilePassword:${java.io.tmpdir}/tmp.password";
        passwordCmd = StringPropertyReplacer.replaceProperties(passwordCmd);
        createVault(passwordCmd);

        Assert.assertEquals(VAULT_ATTRIBUTE, getVaultedPassword());

        removeVault();
        tmpPassword.delete();
    }

    /**
     * Testing command which return wrong password. The vault configuration
     * should fail to create, because of wrong password.
     */
    @Test
    public void testWrongPassword() throws Exception {

        try {
            createVault("{CLASS@" + ExternalPasswordModuleSetupTask.getModuleName() + "}"
                    + ExternalPassword.class.getName() + ":Wrong,Password");
            fail();
        } catch (Exception ex) {
            // OK
        }
    }

    public String getVaultedPassword() throws Exception {

        URI vaultCheckServlet = new URI(url.toExternalForm() + CheckVaultedPassServlet.SERVLET_PATH + "?"
                + CheckVaultedPassServlet.VAULTED_PASS + "=" + VAULTED_PROPERTY);

        return Utils.makeCall(vaultCheckServlet, 200);
    }

    public void createVault(String vaultPassword) throws Exception {

        ModelNode op = new ModelNode();

        op = Util.createAddOperation(VAULT_PATH);
        ModelNode vaultOption = op.get(VAULT_OPTIONS);
        vaultOption.get("KEYSTORE_URL").set(KEYSTORE_URL);
        vaultOption.get("KEYSTORE_PASSWORD").set(vaultPassword);
        vaultOption.get("KEYSTORE_ALIAS").set(VAULT_ALIAS);
        vaultOption.get("SALT").set(SALT);
        vaultOption.get("ITERATION_COUNT").set(Integer.toString(ITER_COUNT));
        vaultOption.get("ENC_FILE_DIR").set(RESOURCE_LOCATION);
        Utils.applyUpdate(op, managementClient.getControllerClient());

        LOGGER.debug("Vault created in server configuration");

    }

    public void removeVault() throws Exception {
        ModelNode op = Util.createRemoveOperation(VAULT_PATH);
        Utils.applyUpdate(op, managementClient.getControllerClient());
    }

    /**
     * Creates runnable command for vault to retrieve password from external
     * source
     *
     * @param option    as command type EXT or CMD
     * @param delimiter to split argument from command
     * @return command to run a class which will return password
     */
    public String buildCommand(String option, String delimiter) {
        // First check for java.exe or java as the binary
        File java = new File(System.getProperty("java.home"), "/bin/java");
        File javaExe = new File(System.getProperty("java.home"), "/bin/java.exe");
        String jre;
        if (java.exists()) { jre = java.getAbsolutePath(); } else { jre = javaExe.getAbsolutePath(); }
        // Build the command to run this jre
        String cmd = jre + delimiter + "-cp" + delimiter
                + ExternalPasswordProvider.class.getProtectionDomain().getCodeSource().getLocation().getPath() + delimiter
                + "org.jboss.as.test.integration.security.loginmodules.ExternalPasswordProvider" + delimiter
                + passwordProvider.getCounterFile() + delimiter + VAULT_PASSWORD;

        return "{" + option + "}" + cmd;
    }

    static class ExternalVaultPasswordSetup implements ServerSetupTask {

        private ModelNode originalVault;
        private VaultSession nonInteractiveSession;
        private VaultHandler vaultHandler;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            passwordProvider.resetFileCounter();

            ModelNode op = new ModelNode();

            // save original vault setting
            LOGGER.trace("Saving original vault setting");
            op = Util.getReadAttributeOperation(VAULT_PATH, VAULT_OPTIONS);
            originalVault = (managementClient.getControllerClient().execute(new OperationBuilder(op).build())).get(RESULT);

            // remove original vault
            if (originalVault.get("KEYSTORE_URL") != null && originalVault.hasDefined("KEYSTORE_URL")) {
                op = Util.createRemoveOperation(VAULT_PATH);
                Utils.applyUpdate(op, managementClient.getControllerClient());
            }

            // create new vault
            LOGGER.trace("Creating new vault");
            clean();
            vaultHandler = new VaultHandler(KEYSTORE_URL, VAULT_PASSWORD, null, RESOURCE_LOCATION, 128, VAULT_ALIAS, SALT, ITER_COUNT);

            KEYSTORE_URL = vaultHandler.getKeyStore();
            RESOURCE_LOCATION = vaultHandler.getEncodedVaultFileDirectory();

            nonInteractiveSession = new VaultSession(KEYSTORE_URL, VAULT_PASSWORD, RESOURCE_LOCATION, SALT, ITER_COUNT);
            nonInteractiveSession.startVaultSession(VAULT_ALIAS);

            // create security attributes
            LOGGER.trace("Inserting attribute " + VAULT_ATTRIBUTE + " to vault");
            nonInteractiveSession.addSecuredAttribute(VAULT_BLOCK, ATTRIBUTE_NAME, VAULT_ATTRIBUTE.toCharArray());

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            ModelNode op;

            // set original vault
            if (originalVault.get("KEYSTORE_URL") != null && originalVault.hasDefined("KEYSTORE_URL")) {
                Set<String> originalVaultParam = originalVault.keys();
                Iterator<String> it = originalVaultParam.iterator();
                op = Util.createAddOperation(VAULT_PATH);
                ModelNode vaultOption = op.get(VAULT_OPTIONS);
                while (it.hasNext()) {
                    String param = it.next();
                    vaultOption.get(param).set(originalVault.get(param));
                }
                Utils.applyUpdate(op, managementClient.getControllerClient());
            }

            // remove vault files
            vaultHandler.cleanUp();
            // remove password helper file
            passwordProvider.cleanup();
        }

        private void clean() throws IOException{

            File datFile = new File(VAULT_DAT_FILE);
            if (datFile.exists()) {
                Files.delete(datFile.toPath());
            }
        }
    }

}
