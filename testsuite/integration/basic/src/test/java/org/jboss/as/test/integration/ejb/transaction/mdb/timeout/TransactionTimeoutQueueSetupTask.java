/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.mdb.timeout;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;

public class TransactionTimeoutQueueSetupTask implements ServerSetupTask {

    public static final String NO_TIMEOUT_QUEUE_NAME = "noTimeoutQueue";
    public static final String NO_TIMEOUT_JNDI_NAME = "queue/" + NO_TIMEOUT_QUEUE_NAME;
    public static final String DEFAULT_TIMEOUT_QUEUE_NAME = "defaultTimeoutQueue";
    public static final String DEFAULT_TIMEOUT_JNDI_NAME = "queue/" + DEFAULT_TIMEOUT_QUEUE_NAME;
    public static final String ANNOTATION_TIMEOUT_QUEUE_NAME = "annotationTimeoutQueue";
    public static final String ANNOTATION_TIMEOUT_JNDI_NAME = "queue/" + ANNOTATION_TIMEOUT_QUEUE_NAME;
    public static final String PROPERTY_TIMEOUT_QUEUE_NAME = "propertyTimeoutQueue";
    public static final String PROPERTY_TIMEOUT_JNDI_NAME = "queue/" + PROPERTY_TIMEOUT_QUEUE_NAME;

    public static final String REPLY_QUEUE_NAME = "replyQueue";
    public static final String REPLY_QUEUE_JNDI_NAME = "queue/" + REPLY_QUEUE_NAME;


    private JMSOperations adminOperations;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        adminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        adminOperations.createJmsQueue(NO_TIMEOUT_QUEUE_NAME, NO_TIMEOUT_JNDI_NAME);
        adminOperations.createJmsQueue(DEFAULT_TIMEOUT_QUEUE_NAME, DEFAULT_TIMEOUT_JNDI_NAME);
        adminOperations.createJmsQueue(ANNOTATION_TIMEOUT_QUEUE_NAME, ANNOTATION_TIMEOUT_JNDI_NAME);
        adminOperations.createJmsQueue(PROPERTY_TIMEOUT_QUEUE_NAME, PROPERTY_TIMEOUT_JNDI_NAME);
        adminOperations.createJmsQueue(REPLY_QUEUE_NAME, REPLY_QUEUE_JNDI_NAME);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (adminOperations != null) {
            try {
                adminOperations.removeJmsQueue(NO_TIMEOUT_QUEUE_NAME);
                adminOperations.removeJmsQueue(DEFAULT_TIMEOUT_QUEUE_NAME);
                adminOperations.removeJmsQueue(ANNOTATION_TIMEOUT_QUEUE_NAME);
                adminOperations.removeJmsQueue(PROPERTY_TIMEOUT_QUEUE_NAME);
                adminOperations.removeJmsQueue(REPLY_QUEUE_NAME);
            } finally {
                adminOperations.close();
            }
        }
    }

}