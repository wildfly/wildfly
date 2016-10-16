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

package org.jboss.as.test.integration.security.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.vault.VaultSession;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Basic Vault ServerSetupTask which add new vault and store attribute for block "someVaultBlock" and attribute name
 * "someAttributeName" with attribute value "secretValue"
 *
 * @author olukas
 *
 */
public class BasicVaultServerSetupTask implements ServerSetupTask {

    private static Logger LOGGER = Logger.getLogger(BasicVaultServerSetupTask.class);

    private ModelNode originalVault;
    private VaultSession nonInteractiveSession;

    public static final String ATTRIBUTE_NAME = "someAttributeName";
    public static final String VAULT_BLOCK = "someVaultBlock";
    public static final String VAULT_ATTRIBUTE = "secretValue";
    public static final String VAULTED_PROPERTY = "${VAULT::" + VAULT_BLOCK + "::" + ATTRIBUTE_NAME + "::1}";
    public static final String VAULT_PASSWORD = "VaultPassword";
    public static final String VAULT_ALIAS = "VaultAlias";

    static final String KEY_STORE_FILE = "myVault.keystore";
    static final String RESOURCE_LOCATION = "";

    static final PathAddress VAULT_PATH = PathAddress.pathAddress().append(CORE_SERVICE, VAULT);

    private VaultHandler vaultHandler;

    private String externalVaultPassword = null;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {

        // clean directory and keystore
        VaultHandler.cleanFilesystem(RESOURCE_LOCATION, false, KEY_STORE_FILE);

        // create vault keystore
        vaultHandler = new VaultHandler(KEY_STORE_FILE, VAULT_PASSWORD, null, RESOURCE_LOCATION, 128, VAULT_ALIAS,
                "87654321", 20);

        ModelNode op = new ModelNode();

        // save original vault setting
        LOGGER.trace("Saving original vault setting");
        op = Util.getReadAttributeOperation(VAULT_PATH, VAULT_OPTIONS);
        originalVault = (managementClient.getControllerClient().execute(new OperationBuilder(op).build())).get(RESULT);

        // remove original vault
        if (originalVault.get("KEYSTORE_URL") != null && originalVault.hasDefined("KEYSTORE_URL")) {
            op = Util.createRemoveOperation(VAULT_PATH);
            CoreUtils.applyUpdate(op, managementClient.getControllerClient());
        }

        // create new vault
        LOGGER.trace("Creating new vault");
        String keystoreURL = vaultHandler.getKeyStore();
        String encryptionDirectory = new File(RESOURCE_LOCATION).getAbsolutePath();
        String salt = "87654321";
        int iterationCount = 20;

        nonInteractiveSession = new VaultSession(keystoreURL, VAULT_PASSWORD, encryptionDirectory, salt, iterationCount);
        nonInteractiveSession.startVaultSession(VAULT_ALIAS);

        // create security attributes
        LOGGER.trace("Inserting attribute " + VAULT_ATTRIBUTE + " to vault");
        nonInteractiveSession.addSecuredAttribute(VAULT_BLOCK, ATTRIBUTE_NAME, VAULT_ATTRIBUTE.toCharArray());

        // create new vault setting in standalone
        op = Util.createAddOperation(VAULT_PATH);
        ModelNode vaultOption = op.get(VAULT_OPTIONS);
        vaultOption.get("KEYSTORE_URL").set(keystoreURL);
        if (externalVaultPassword != null) {
            vaultOption.get("KEYSTORE_PASSWORD").set(externalVaultPassword);
        } else {
            vaultOption.get("KEYSTORE_PASSWORD").set(nonInteractiveSession.getKeystoreMaskedPassword());
        }
        vaultOption.get("KEYSTORE_ALIAS").set(VAULT_ALIAS);
        vaultOption.get("SALT").set(salt);
        vaultOption.get("ITERATION_COUNT").set(Integer.toString(iterationCount));
        vaultOption.get("ENC_FILE_DIR").set(encryptionDirectory);
        CoreUtils.applyUpdate(op, managementClient.getControllerClient());

        LOGGER.debug("Vault created in server configuration");

    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

        ModelNode op;

        op = Util.createRemoveOperation(VAULT_PATH);
        CoreUtils.applyUpdate(op, managementClient.getControllerClient());

        // set original vault
        if (originalVault.get("KEYSTORE_URL") != null && originalVault.hasDefined("KEYSTORE_URL")) {
            Set<String> originalVaultParam = originalVault.keys();
            Iterator<String> it = originalVaultParam.iterator();
            op = Util.createAddOperation(VAULT_PATH);
            ModelNode vaultOption = op.get(VAULT_OPTIONS);
            while (it.hasNext()) {
                String param = (String) it.next();
                vaultOption.get(param).set(originalVault.get(param));
            }
            CoreUtils.applyUpdate(op, managementClient.getControllerClient());
        }

        // remove vault files
        vaultHandler.cleanUp();
    }

    protected void setExternalVaultPassword(String externalVaultPassword) {
        this.externalVaultPassword = externalVaultPassword;
    }
}
