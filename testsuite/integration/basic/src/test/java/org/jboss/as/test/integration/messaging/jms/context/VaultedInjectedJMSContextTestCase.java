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

package org.jboss.as.test.integration.messaging.jms.context;

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
import static org.jboss.as.test.shared.TimeoutUtil.adjust;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.SocketPermission;
import java.util.PropertyPermission;
import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.TemporaryQueue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.messaging.jms.context.auxiliary.VaultedMessageProducer;
import org.jboss.as.test.integration.security.common.VaultHandler;
import org.jboss.as.test.shared.TimeoutUtil;
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
@ServerSetup(VaultedInjectedJMSContextTestCase.StoreVaultedPropertyTask.class)
public class VaultedInjectedJMSContextTestCase {

    static final String VAULT_LOCATION = VaultedInjectedJMSContextTestCase.class.getResource("/").getPath() + "security/jms-vault/";

    static class StoreVaultedPropertyTask implements ServerSetupTask {

        private VaultHandler vaultHandler;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            VaultHandler.cleanFilesystem(VAULT_LOCATION, true);

            // create new vault
            vaultHandler = new VaultHandler(VAULT_LOCATION);
            // store the destination lookup into the vault
            String vaultedUserName = vaultHandler.addSecuredAttribute("messaging", "userName", "guest".toCharArray());
            String vaultedPassword = vaultHandler.addSecuredAttribute("messaging", "password", "guest".toCharArray());

            addVaultConfiguration(managementClient);

            updateAnnotationPropertyReplacement(managementClient, true);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            removeVaultConfiguration(managementClient);
            // remove temporary files
            vaultHandler.cleanUp();

            updateAnnotationPropertyReplacement(managementClient, false);
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

        private void updateAnnotationPropertyReplacement(ManagementClient managementClient, boolean value) throws IOException {
            ModelNode op;
            op = new ModelNode();
            op.get(OP_ADDR).add("subsystem", "ee");
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("annotation-property-replacement");
            op.get(VALUE).set(value);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());
        }
    }

    @Resource(mappedName = "/JmsXA")
    private ConnectionFactory factory;

    @EJB
    private VaultedMessageProducer producerBean;

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "VaultedInjectedJMSContextTestCase.jar")
                .addClass(TimeoutUtil.class)
                .addPackage(VaultedMessageProducer.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(createPermissionsXmlAsset(
                        new PropertyPermission("ts.timeout.factor", "read"),
                        // required because the VaultedMessageProducer uses the RemoteConnectionFactory
                        // (that requires auth with vaulted credentials)
                        new SocketPermission("*", "connect"),
                        new RuntimePermission("setContextClassLoader")), "META-INF/jboss-permissions.xml");
    }

    @Test
    public void sendMessage() throws JMSException {
        String text = UUID.randomUUID().toString();

        try (JMSContext context = factory.createContext()) {

            TemporaryQueue tempQueue = context.createTemporaryQueue();

            producerBean.sendToDestination(tempQueue, text);

            JMSConsumer consumer = context.createConsumer(tempQueue);
            String reply = consumer.receiveBody(String.class, adjust(2000));
            assertEquals(text, reply);
        }
    }
}
