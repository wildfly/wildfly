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

package org.jboss.as.test.integration.messaging.jms.definitions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.shrinkwrap.api.ArchivePaths.create;

import java.io.IOException;

import javax.ejb.EJB;
import javax.jms.JMSException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.security.common.VaultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(JMSResourceDefinitionsTestCase.StoreVaultedPropertyTask.class)
public class JMSResourceDefinitionsTestCase {

    static final String VAULT_LOCATION = JMSResourceDefinitionsTestCase.class.getResource("/").getPath() + "security/jms-vault/";

    static class StoreVaultedPropertyTask implements ServerSetupTask {

        private VaultHandler vaultHandler;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            VaultHandler.cleanFilesystem(VAULT_LOCATION, true);

            // create new vault
            vaultHandler = new VaultHandler(VAULT_LOCATION);
            // store the destination lookup into the vault
            String vaultedUserName = vaultHandler.addSecuredAttribute("messaging", "userName", "guest".toCharArray());
            //System.out.println("vaultedUserName = " + vaultedUserName);
            String vaultedPassword = vaultHandler.addSecuredAttribute("messaging", "password", "guest".toCharArray());
            //System.out.println("vaultedPassword = " + vaultedPassword);

            addVaultConfiguration(managementClient);

            // for annotation-based JMS definitions
            updatePropertyReplacement(managementClient, "annotation-property-replacement", true);
            // for deployment descriptor-based JMS definitions
            updatePropertyReplacement(managementClient, "spec-descriptor-property-replacement", true);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            removeVaultConfiguration(managementClient);
            // remove temporary files
            vaultHandler.cleanUp();

            updatePropertyReplacement(managementClient, "annotation-property-replacement", false);
            updatePropertyReplacement(managementClient, "spec-descriptor-property-replacement", false);
        }


        private void addVaultConfiguration(ManagementClient managementClient) throws IOException {
            ModelNode op;
            op = new ModelNode();
            op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
            op.get(OP).set(ADD);
            ModelNode vaultOption = op.get(VAULT_OPTIONS);
            vaultOption.get("KEYSTORE_URL").set(vaultHandler.getKeyStore());
            vaultOption.get("KEYSTORE_PASSWORD").set(vaultHandler.getMaskedKeyStorePassword());
            vaultOption.get("KEYSTORE_ALIAS").set(vaultHandler.getAlias());
            vaultOption.get("SALT").set(vaultHandler.getSalt());
            vaultOption.get("ITERATION_COUNT").set(vaultHandler.getIterationCountAsString());
            vaultOption.get("ENC_FILE_DIR").set(vaultHandler.getEncodedVaultFileDirectory());
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());
        }

        private void removeVaultConfiguration(ManagementClient managementClient) throws IOException {
            ModelNode op = new ModelNode();
            op.get(OP_ADDR).add(CORE_SERVICE, VAULT);
            op.get(OP).set(REMOVE);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());
        }

        private void updatePropertyReplacement(ManagementClient managementClient, String propertyReplacement, boolean value) throws IOException {
            ModelNode op;
            op = new ModelNode();
            op.get(OP_ADDR).add("subsystem", "ee");
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set(propertyReplacement);
            op.get(VALUE).set(value);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());
        }
    }

    @EJB
    private MessagingBean bean;

    @Deployment
    public static JavaArchive createArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "JMSResourceDefinitionsTestCase.jar")
                .addPackage(MessagingBean.class.getPackage())
                .addAsManifestResource(
                        MessagingBean.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        create("beans.xml"));
        return archive;
    }

    @Test
    public void testInjectedDefinitions() throws JMSException {
        bean.checkInjectedResources();
    }

    @Test
    public void testAnnotationBasedDefinitionsWithVaultedAttributes() throws JMSException {
        bean.checkAnnotationBasedDefinitionsWithVaultedAttributes();
    }

    @Test
    public void testDeploymendDescriptorBasedDefinitionsWithVaultedAttributes() throws JMSException {
        bean.checkDeploymendDescriptorBasedDefinitionsWithVaultedAttributes();
    }

}
