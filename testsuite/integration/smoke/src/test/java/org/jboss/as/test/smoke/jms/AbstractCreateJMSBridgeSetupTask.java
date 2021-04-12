/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.jms;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.dmr.ModelNode;

/**
 * Abstract Setup task to create/remove a Jakarta Messaging bridge.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public abstract class AbstractCreateJMSBridgeSetupTask extends CreateQueueSetupTask {

    public static final String CF_NAME = "myAwesomeCF";
    public static final String CF_JNDI_NAME = "/myAwesomeCF";
    public static final String JMS_BRIDGE_NAME = "myAwesomeJMSBridge";

    private JMSOperations jmsOperations;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        super.setup(managementClient, containerId);
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

        ModelNode connectionFactoryAttributes = new ModelNode();
        connectionFactoryAttributes.get("connectors").add("in-vm");
        connectionFactoryAttributes.get("factory-type").set("XA_GENERIC");
        jmsOperations.addJmsConnectionFactory(CF_NAME, CF_JNDI_NAME, connectionFactoryAttributes);

        ModelNode jmsBridgeAttributes = new ModelNode();
        jmsBridgeAttributes.get("source-connection-factory").set(CF_JNDI_NAME);
        jmsBridgeAttributes.get("source-destination").set(QUEUE1_JNDI_NAME);
        jmsBridgeAttributes.get("target-connection-factory").set(CF_JNDI_NAME);
        jmsBridgeAttributes.get("target-destination").set(QUEUE2_JNDI_NAME);
        jmsBridgeAttributes.get("module").set("org.apache.activemq.artemis:main");
        configureBridge(jmsBridgeAttributes);
        jmsOperations.addJmsBridge(JMS_BRIDGE_NAME, jmsBridgeAttributes);
    }


    protected abstract void configureBridge(ModelNode jmsBridgeAttributes);

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        jmsOperations.removeJmsBridge(JMS_BRIDGE_NAME);
        jmsOperations.removeJmsConnectionFactory(CF_NAME);
        super.tearDown(managementClient, containerId);
    }
}
