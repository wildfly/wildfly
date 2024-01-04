/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
