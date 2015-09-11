/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.activationconfig.unit;

import static org.jboss.as.test.integration.ejb.mdb.activationconfig.JMSHelper.assertSendAndReceiveTextMessage;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;
import java.util.UUID;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.as.test.integration.ejb.mdb.activationconfig.MDBWithLookupActivationConfigProperties;
import org.jboss.as.test.integration.ejb.mdb.activationconfig.MDBWithUnknownActivationConfigProperties;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests activation config properties on a MDB
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup({MDBActivationConfigTestCase.JmsQueueSetup.class})
public class MDBActivationConfigTestCase {

    private static final Logger logger = Logger.getLogger(MDBActivationConfigTestCase.class);

    @Resource(mappedName = MDBWithUnknownActivationConfigProperties.QUEUE_JNDI_NAME)
    private Queue queue;

    @Resource(mappedName = MDBWithLookupActivationConfigProperties.QUEUE_JNDI_NAME)
    private Queue queue2;

    @Resource(mappedName = "/ConnectionFactory")
    private ConnectionFactory cf;

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("mdbtest/activation-config-queue", MDBWithUnknownActivationConfigProperties.QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("mdbtest/activation-config-queue2", MDBWithLookupActivationConfigProperties.QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("mdbtest/activation-config-queue");
                jmsAdminOperations.removeJmsQueue("mdbtest/activation-config-queue2");
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static Archive getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "MDBActivationConfigTestCase.jar")
                .addPackage(MDBWithUnknownActivationConfigProperties.class.getPackage())
                .addClasses(JMSOperations.class, JMSMessagingUtil.class, JmsQueueSetup.class)
                .addClasses(TimeoutUtil.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF")
                .addAsResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
    }

    /**
     * Tests that deployment of a MDB containing an unknown/unsupported activation config property doesn't throw
     * a deployment error.
     *
     * @throws Exception
     */
    @Test
    public void testUnrecognizedActivationConfigProps() throws Exception {
        String text = UUID.randomUUID().toString();

        assertSendAndReceiveTextMessage(cf, queue, text);
    }

    /**
     * Test that deployment of a MDB can use the connectionFactoryLookup and destinationLookup activation config properties.
     *
     * @throws Exception
     */
    @Test
    public void testConnectionFactoryLookupActivationConfigProperty() throws Exception {
        String text = UUID.randomUUID().toString();

        assertSendAndReceiveTextMessage(cf, queue2, text);
    }
}
