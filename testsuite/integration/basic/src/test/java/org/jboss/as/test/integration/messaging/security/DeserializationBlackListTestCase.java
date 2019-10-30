/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.security;

import static org.jboss.as.test.integration.messaging.security.DeserializationMessagingBean.BLACK_LIST_CF_LOOKUP;
import static org.jboss.as.test.integration.messaging.security.DeserializationMessagingBean.BLACK_LIST_REGULAR_CF_LOOKUP;
import static org.jboss.as.test.integration.messaging.security.DeserializationMessagingBean.WHITE_LIST_CF_LOOKUP;
import static org.jboss.shrinkwrap.api.ArchivePaths.create;

import java.util.Date;
import java.util.UUID;

import javax.ejb.EJB;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
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
@ServerSetup(DeserializationBlackListTestCase.SetupTask.class)
public class DeserializationBlackListTestCase {

    static class SetupTask implements ServerSetupTask {

        private static final String CF_NAME = "myBlackListCF";

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode attributes = new ModelNode();
            attributes.get("connectors").add("in-vm");
            attributes.get("deserialization-black-list").add(new ModelNode("*"));
            JMSOperationsProvider.getInstance(managementClient.getControllerClient())
                    .addJmsConnectionFactory(CF_NAME, BLACK_LIST_REGULAR_CF_LOOKUP, attributes);
        }


        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperationsProvider.getInstance(managementClient.getControllerClient()).removeJmsConnectionFactory(CF_NAME);
        }
    }

    @Deployment
    public static JavaArchive createArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "DeserializationBlackListTestCase.jar")
                .addClass(DeserializationMessagingBean.class)
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        create("beans.xml"));

        return archive;
    }

    @EJB
    private DeserializationMessagingBean bean;

    @Test
    public void testDeserializationBlackList() throws NamingException {
        // UUID is black listed, any other Serializable must be deserialized.
        UUID uuid = UUID.randomUUID();
        Date date = new Date();

        bean.send(uuid);
        bean.receive(uuid, BLACK_LIST_CF_LOOKUP,true);
        bean.send(date);
        bean.receive(date, BLACK_LIST_CF_LOOKUP,false);
    }

    @Test
    public void testDeserializationBlackListFromRegularConnectionFactory() throws NamingException {
        // all classes are black listed
        UUID uuid = UUID.randomUUID();
        Date date = new Date();

        bean.send(uuid);
        bean.receive(uuid, BLACK_LIST_REGULAR_CF_LOOKUP,true);
        bean.send(date);
        bean.receive(date, BLACK_LIST_REGULAR_CF_LOOKUP,true);
    }

    @Test
    public void testDeserializationWhiteList() throws NamingException {
        // UUID is white listed, any other Serializable must not be deserialized.
        UUID uuid = UUID.randomUUID();
        Date date = new Date();

        bean.send(uuid);
        bean.receive(uuid, WHITE_LIST_CF_LOOKUP,false);
        bean.send(date);
        bean.receive(date, WHITE_LIST_CF_LOOKUP,true);
    }

}
