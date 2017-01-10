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
public class CreateQueueSetupTask implements ServerSetupTask {

    public static final String QUEUE1_NAME = "myAwesomeQueue";
    public static final String QUEUE1_JNDI_NAME = "queue/myAwesomeQueue";
    public static final String QUEUE2_NAME = "myAwesomeQueue2";
    public static final String QUEUE2_JNDI_NAME = "queue/myAwesomeQueue2";
    public static final String QUEUE3_NAME = "myAwesomeQueue3";
    public static final String QUEUE3_JNDI_NAME = "queue/myAwesomeQueue3";


    private JMSOperations adminOperations;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        adminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        adminOperations.createJmsQueue(QUEUE1_NAME, QUEUE1_JNDI_NAME);
        adminOperations.createJmsQueue(QUEUE2_NAME, QUEUE2_JNDI_NAME);
        adminOperations.createJmsQueue(QUEUE3_NAME, QUEUE3_JNDI_NAME);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (adminOperations != null) {
            adminOperations.removeJmsQueue(QUEUE1_NAME);
            adminOperations.removeJmsQueue(QUEUE2_NAME);
            adminOperations.removeJmsQueue(QUEUE3_NAME);
            adminOperations.close();
        }
    }

}
