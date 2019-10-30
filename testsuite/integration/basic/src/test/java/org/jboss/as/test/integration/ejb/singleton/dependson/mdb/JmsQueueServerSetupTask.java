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

package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;

/**
 * @author baranowb
 */
public class JmsQueueServerSetupTask implements ServerSetupTask {

    private JMSOperations jmsAdminOperations;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
        jmsAdminOperations.createJmsQueue(Constants.QUEUE_NAME, Constants.QUEUE_JNDI_NAME);
        jmsAdminOperations.createJmsQueue(Constants.QUEUE_REPLY_NAME, Constants.QUEUE_REPLY_JNDI_NAME);
        jmsAdminOperations.setSystemProperties(Constants.SYS_PROP_KEY, Constants.SYS_PROP_VALUE);

    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (jmsAdminOperations != null) {
            jmsAdminOperations.removeJmsQueue(Constants.QUEUE_NAME);
            jmsAdminOperations.removeJmsQueue(Constants.QUEUE_REPLY_NAME);
            jmsAdminOperations.removeSystemProperties();
            jmsAdminOperations.close();
        }
    }
}
