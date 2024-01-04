/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
