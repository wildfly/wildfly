/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.definitions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.shrinkwrap.api.ArchivePaths.create;
import static org.wildfly.test.security.common.SecureExpressionUtil.getDeploymentPropertiesAsset;
import static org.wildfly.test.security.common.SecureExpressionUtil.setupCredentialStore;
import static org.wildfly.test.security.common.SecureExpressionUtil.setupCredentialStoreExpressions;
import static org.wildfly.test.security.common.SecureExpressionUtil.teardownCredentialStore;

import java.io.IOException;
import java.net.SocketPermission;

import jakarta.ejb.EJB;
import jakarta.jms.JMSException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.shared.PermissionUtils;
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
@ServerSetup(JMSResourceDefinitionsTestCase.StoreVaultedPropertyTask.class)
public class JMSResourceDefinitionsTestCase {

    static final String UNIQUE_NAME = "JMSResourceDefinitionsTestCase";
    private static final SecureExpressionUtil.SecureExpressionData USERNAME = new SecureExpressionUtil.SecureExpressionData("guest", "test.userName");
    private static final SecureExpressionUtil.SecureExpressionData PASSWORD = new SecureExpressionUtil.SecureExpressionData("guest", "test.password");
    static final String STORE_LOCATION = JMSResourceDefinitionsTestCase.class.getResource("/").getPath() + "security/" + UNIQUE_NAME + ".cs";

    static class StoreVaultedPropertyTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            setupCredentialStore(managementClient, UNIQUE_NAME, STORE_LOCATION);
            // for annotation-based JMS definitions
            updatePropertyReplacement(managementClient, "annotation-property-replacement", true);
            // for deployment descriptor-based JMS definitions
            updatePropertyReplacement(managementClient, "spec-descriptor-property-replacement", true);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            updatePropertyReplacement(managementClient, "annotation-property-replacement", false);
            updatePropertyReplacement(managementClient, "spec-descriptor-property-replacement", false);
            teardownCredentialStore(managementClient, UNIQUE_NAME, STORE_LOCATION);
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
    public static JavaArchive createArchive() throws Exception {

        // Create the credential expressions so we can store them in the deployment
        setupCredentialStoreExpressions(UNIQUE_NAME, USERNAME, PASSWORD);

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "JMSResourceDefinitionsTestCase.jar")
                .addPackage(MessagingBean.class.getPackage())
                .addClasses(SecureExpressionUtil.getDeploymentClasses())
                .addAsManifestResource(
                        MessagingBean.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new SocketPermission("localhost", "resolve")), "permissions.xml")
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        create("beans.xml"))
                .addAsManifestResource(getDeploymentPropertiesAsset(USERNAME, PASSWORD), "jboss.properties");
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
