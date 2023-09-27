/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.security;

import static org.jboss.as.test.integration.messaging.security.DeserializationMessagingBean.BLACK_LIST_CF_LOOKUP;
import static org.jboss.as.test.integration.messaging.security.DeserializationMessagingBean.BLACK_LIST_REGULAR_CF_LOOKUP;
import static org.jboss.as.test.integration.messaging.security.DeserializationMessagingBean.WHITE_LIST_CF_LOOKUP;
import static org.jboss.shrinkwrap.api.ArchivePaths.create;

import java.util.Date;
import java.util.UUID;

import jakarta.ejb.EJB;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(DeserializationBlockListTestCase.SetupTask.class)
public class DeserializationBlockListTestCase {

    static class SetupTask implements ServerSetupTask {

        private static final String CF_NAME = "myBlockListCF";
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(new ModelNode().add("subsystem", "ee"), "annotation-property-replacement", true));
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            ModelNode attributes = new ModelNode();
            if (ops.isRemoteBroker()) {
                attributes.get("connectors").add("artemis");
            } else {
                attributes.get("connectors").add("in-vm");
            }
            attributes.get("deserialization-block-list").add(new ModelNode("*"));
            ops.addJmsConnectionFactory(CF_NAME, BLACK_LIST_REGULAR_CF_LOOKUP, attributes);
        }


        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            ops.removeJmsConnectionFactory(CF_NAME);
            managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(new ModelNode().add("subsystem", "ee"), "annotation-property-replacement", ops.isRemoteBroker()));
        }
    }

    @Deployment
    public static JavaArchive createArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "DeserializationBlockListTestCase.jar")
                .addClass(DeserializationMessagingBean.class)
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        create("beans.xml"));

        return archive;
    }

    @EJB
    private DeserializationMessagingBean bean;

    @Test
    public void testDeserializationBlockList() throws NamingException {
        // UUID is block listed, any other Serializable must be deserialized.
        UUID uuid = UUID.randomUUID();
        Date date = new Date();

        bean.send(uuid);
        bean.receive(uuid, BLACK_LIST_CF_LOOKUP,true);
        bean.send(date);
        bean.receive(date, BLACK_LIST_CF_LOOKUP,false);
    }

    @Test
    public void testDeserializationBlockListFromRegularConnectionFactory() throws NamingException {
        // all classes are block listed
        UUID uuid = UUID.randomUUID();
        Date date = new Date();

        bean.send(uuid);
        bean.receive(uuid, BLACK_LIST_REGULAR_CF_LOOKUP,true);
        bean.send(date);
        bean.receive(date, BLACK_LIST_REGULAR_CF_LOOKUP,true);
    }

    @Test
    public void testDeserializationAllowList() throws NamingException {
        // UUID is allow listed, any other Serializable must not be deserialized.
        UUID uuid = UUID.randomUUID();
        Date date = new Date();

        bean.send(uuid);
        bean.receive(uuid, WHITE_LIST_CF_LOOKUP,false);
        bean.send(date);
        bean.receive(date, WHITE_LIST_CF_LOOKUP,true);
    }

}
