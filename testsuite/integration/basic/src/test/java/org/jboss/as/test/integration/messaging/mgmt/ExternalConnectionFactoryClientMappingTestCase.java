/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.messaging.mgmt;

import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import java.io.IOException;
import java.util.Map;

import static org.jboss.as.controller.client.helpers.ClientConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;

/**
 * Created by spyrkob on 18/05/2017.
 */
@RunWith(Arquillian.class)
@ServerSetup({ExternalConnectionFactoryClientMappingTestCase.SetupTask.class})
public class ExternalConnectionFactoryClientMappingTestCase {

    private static final String CONNECTION_FACTORY_JNDI_NAME = "java:jboss/exported/jms/TestConnectionFactory";

    static class SetupTask extends SnapshotRestoreSetupTask {
        private static final Logger logger = Logger.getLogger(ExternalConnectionFactoryClientMappingTestCase.SetupTask.class);

        @Override
        public void doSetup(org.jboss.as.arquillian.container.ManagementClient managementClient, String s) throws Exception {
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

            addSocketBinding(managementClient, "test-binding", clientMapping("test", "8000"));
            ops.addExternalHttpConnector("http-test-connector", "test-binding", "http-acceptor");
            ModelNode attr = new ModelNode();
            attr.get("connectors").add("http-test-connector");
            ops.addJmsExternalConnectionFactory("TestConnectionFactory", CONNECTION_FACTORY_JNDI_NAME, attr);

            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }

        private ModelNode clientMapping(String destAddr, String destPort) {
            ModelNode clientMapping = new ModelNode();
            clientMapping.get("destination-address").set(destAddr);
            clientMapping.get("destination-port").set(destPort);
            return clientMapping;
        }

        private void addSocketBinding(ManagementClient managementClient, String bindingName, ModelNode clientMapping) throws Exception {
            ModelNode address = new ModelNode();
            address.add("socket-binding-group", "standard-sockets");
            address.add("socket-binding", bindingName);

            ModelNode socketBindingOp = new ModelNode();
            socketBindingOp.get(OP).set(ADD);
            socketBindingOp.get(OP_ADDR).set(address);

            execute(managementClient, socketBindingOp);

            ModelNode clientMappingOp = new ModelNode();
            clientMappingOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            clientMappingOp.get(OP_ADDR).set(address);
            clientMappingOp.get(NAME).set("client-mappings");
            clientMappingOp.get(VALUE).add(clientMapping);

            execute(managementClient, clientMappingOp);
        }

        static void execute(ManagementClient managementClient, final ModelNode operation) throws IOException {
            ModelNode result = managementClient.getControllerClient().execute(operation);
            if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
                logger.trace("Operation successful for update = " + operation.toString());
            } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
                final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
                throw new RuntimeException(failureDesc);
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
            }
        }

    }

    @Deployment(testable = true)
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test.jar");
        jar.addClass(ExternalConnectionFactoryClientMappingTestCase.class);
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
        params.forEach((s, o) -> System.out.println(s + ": " + o));
        assertEquals("test", params.get(TransportConstants.HOST_PROP_NAME));
        assertEquals(8000, params.get(TransportConstants.PORT_PROP_NAME));
    }
}
