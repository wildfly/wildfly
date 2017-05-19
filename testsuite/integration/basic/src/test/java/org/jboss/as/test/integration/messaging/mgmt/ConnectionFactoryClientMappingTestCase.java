package org.jboss.as.test.integration.messaging.mgmt;

import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by spyrkob on 18/05/2017.
 */
@RunWith(Arquillian.class)
@ServerSetup({ConnectionFactoryClientMappingTestCase.SetupTask.class})
public class ConnectionFactoryClientMappingTestCase {

    private static final String CONNECTION_FACTORY_JNDI_NAME = "java:jboss/exported/jms/TestConnectionFactory";

    static class SetupTask implements ServerSetupTask {
        @Override
        public void setup(org.jboss.as.arquillian.container.ManagementClient managementClient, String s) throws Exception {
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

            ops.addSocketBinding("test-binding", clientMapping("test", "8000"));
            ops.addJmsConnector("http-test-connector", "test-binding", "http-acceptor");
            ModelNode attr = new ModelNode();
            attr.get("connectors").add("http-test-connector");
            ops.addJmsConnectionFactory("TestConnectionFactory", CONNECTION_FACTORY_JNDI_NAME, attr);

            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }

        private ModelNode clientMapping(String destAddr, String destPort) {
            ModelNode clientMapping = new ModelNode();
            clientMapping.get("destination-address").set(destAddr);
            clientMapping.get("destination-port").set(destPort);
            return clientMapping;
        }

        @Override
        public void tearDown(org.jboss.as.arquillian.container.ManagementClient managementClient, String s) throws Exception {
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

            ops.removeJmsConnectionFactory("TestConnectionFactory");
            ops.removeJmsConnector("http-test-connector");
            ops.removeSocketBinding("test-binding");

            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }
    }

    @Deployment(testable = true)
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test.jar");
        jar.addClass(ConnectionFactoryClientMappingTestCase.class);
        jar.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");

        jar.add(new StringAsset(
                "<jboss-deployment-structure>\n" +
                        "  <deployment>\n" +
                        "    <dependencies>\n" +
                        "      <module name=\"org.apache.activemq.artemis\"/>\n" +
                        "    </dependencies>\n" +
                        "  </deployment>\n" +
                        "</jboss-deployment-structure>"), "META-INF/jboss-deployment-structure.xml");
        return jar;
    }

    @Resource(lookup = "java:jboss/exported/jms/TestConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Test
    public void testClientMappingInConnectionFactory() throws Exception {
        Map<String, Object> params = ((ActiveMQJMSConnectionFactory) connectionFactory).getStaticConnectors()[0].getParams();

        assertEquals("test", params.get(TransportConstants.HOST_PROP_NAME));
        assertEquals(8000, params.get(TransportConstants.PORT_PROP_NAME));
    }
}
