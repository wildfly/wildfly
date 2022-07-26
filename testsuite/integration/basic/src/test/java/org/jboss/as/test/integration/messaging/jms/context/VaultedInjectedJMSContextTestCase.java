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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.shared.TimeoutUtil.adjust;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.wildfly.test.security.common.SecureExpressionUtil.getDeploymentPropertiesAsset;
import static org.wildfly.test.security.common.SecureExpressionUtil.setupCredentialStore;
import static org.wildfly.test.security.common.SecureExpressionUtil.setupCredentialStoreExpressions;
import static org.wildfly.test.security.common.SecureExpressionUtil.teardownCredentialStore;

import java.io.IOException;
import java.net.SocketPermission;
import java.util.PropertyPermission;
import java.util.UUID;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.TemporaryQueue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.messaging.jms.context.auxiliary.VaultedMessageProducer;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.SecureExpressionUtil;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(VaultedInjectedJMSContextTestCase.StoreVaultedPropertyTask.class)
public class VaultedInjectedJMSContextTestCase {

    static final String UNIQUE_NAME = "VaultedInjectedJMSContextTestCase";
    private static final SecureExpressionUtil.SecureExpressionData USERNAME = new SecureExpressionUtil.SecureExpressionData("guest", "test.userName");
    private static final SecureExpressionUtil.SecureExpressionData PASSWORD = new SecureExpressionUtil.SecureExpressionData("guest", "test.password");
    static final String STORE_LOCATION = VaultedInjectedJMSContextTestCase.class.getResource("/").getPath() + "security/" + UNIQUE_NAME + ".cs";

    static class StoreVaultedPropertyTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            setupCredentialStore(managementClient, UNIQUE_NAME, STORE_LOCATION);
            updateAnnotationPropertyReplacement(managementClient, true);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            teardownCredentialStore(managementClient, UNIQUE_NAME, STORE_LOCATION);
            updateAnnotationPropertyReplacement(managementClient, false);
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
    public static JavaArchive createTestArchive() throws Exception {

        // Create the credential expressions so we can store them in the deployment
        setupCredentialStoreExpressions(UNIQUE_NAME, USERNAME, PASSWORD);

        return ShrinkWrap.create(JavaArchive.class, "VaultedInjectedJMSContextTestCase.jar")
                .addClass(TimeoutUtil.class)
                .addClasses(SecureExpressionUtil.getDeploymentClasses())
                .addPackage(VaultedMessageProducer.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(createPermissionsXmlAsset(
                        new PropertyPermission("ts.timeout.factor", "read"),
                        // required because the VaultedMessageProducer uses the RemoteConnectionFactory
                        // (that requires auth with vaulted credentials)
                        new SocketPermission("*", "connect"),
                        new RuntimePermission("setContextClassLoader")), "META-INF/jboss-permissions.xml")
                .addAsManifestResource(getDeploymentPropertiesAsset(USERNAME, PASSWORD), "jboss.properties");
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
