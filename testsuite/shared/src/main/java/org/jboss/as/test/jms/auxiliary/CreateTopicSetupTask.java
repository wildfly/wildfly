package org.jboss.as.test.jms.auxiliary;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;

/**
 * Author: jmartisk
 * Date: 2/27/12
 * Time: 9:06 AM
 */
public class CreateTopicSetupTask implements ServerSetupTask {

    public static final String TOPIC_NAME = "myAwesomeTopic";
    public static final String TOPIC_JNDI_NAME = "topic/myAwesomeTopic";


    private JMSOperations adminOperations;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        adminOperations = JMSOperationsProvider.getInstance(managementClient);
        adminOperations.createJmsTopic(TOPIC_NAME, TOPIC_JNDI_NAME);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (adminOperations != null) {
            adminOperations.removeJmsTopic(TOPIC_NAME);
            adminOperations.close();
        }
    }
}