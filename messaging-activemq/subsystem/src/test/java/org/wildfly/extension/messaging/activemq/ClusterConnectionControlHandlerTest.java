/*
 * Copyright 2022 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Emmanuel Hugonnet (c) 2021 Red Hat, Inc.
 */
public class ClusterConnectionControlHandlerTest {

    public ClusterConnectionControlHandlerTest() {
    }

    /**
     * Test of formatTopology method, of class ClusterConnectionControlHandler.
     */
    @Test
    public void testFormatTopology() {
        String topology = "topology on Topology@2ec5aa9[owner=ClusterConnectionImpl@1518657274[nodeUUID=b7a794ee-b5af-11ec-ae2f-3ce1a1c35439, connector=TransportConfiguration(name=netty, factory=org-apache-activemq-artemis-core-remoting-impl-netty-NettyConnectorFactory) ?port=5445&useNio=true&"
                + "host=localhost&useNioGlobalWorkerPool=true, address=jms, server=ActiveMQServerImpl::name=default]]:"
                + "b7a794ee-b5af-11ec-ae2f-3ce1a1c35439 => TopologyMember[id=b7a794ee-b5af-11ec-ae2f-3ce1a1c35439, connector=Pair[a=TransportConfiguration(name=netty, factory=org-apache-activemq-artemis-core-remoting-impl-netty-NettyConnectorFactory) ?port=5445&useNio=true&host=localhost&useNioGlobalW"
                + "orkerPool=true, b=null], backupGroupName=group1, scaleDownGroupName=null]"
                + "nodes=1 members=1";
        String expResult = "topology on Topology@2ec5aa9[owner=ClusterConnectionImpl@1518657274[nodeUUID=b7a794ee-b5af-11ec-ae2f-3ce1a1c35439," + System.lineSeparator()
                + "\t\tconnector=TransportConfiguration(name=netty," + System.lineSeparator()
                + "\t\t\tfactory=org-apache-activemq-artemis-core-remoting-impl-netty-NettyConnectorFactory)," + System.lineSeparator()
                + "\t\t{" + System.lineSeparator()
                + "\t\t\tport=5445," + System.lineSeparator()
                + "\t\t\tuseNio=true," + System.lineSeparator()
                + "\t\t\thost=localhost," + System.lineSeparator()
                + "\t\t\tuseNioGlobalWorkerPool=true" + System.lineSeparator()
                + "\t\t}," + System.lineSeparator()
                + "\t\t" + System.lineSeparator()
                + "\t\taddress=jms," + System.lineSeparator()
                + "\t\tserver=ActiveMQServerImpl::name=default" + System.lineSeparator()
                + "\t]" + System.lineSeparator()
                + "]:b7a794ee-b5af-11ec-ae2f-3ce1a1c35439 => TopologyMember[id=b7a794ee-b5af-11ec-ae2f-3ce1a1c35439," + System.lineSeparator()
                + "\tconnector=Pair[a=TransportConfiguration(name=netty," + System.lineSeparator()
                + "\t\t\tfactory=org-apache-activemq-artemis-core-remoting-impl-netty-NettyConnectorFactory)," + System.lineSeparator()
                + "\t\t{" + System.lineSeparator()
                + "\t\t\tport=5445," + System.lineSeparator()
                + "\t\t\tuseNio=true," + System.lineSeparator()
                + "\t\t\thost=localhost," + System.lineSeparator()
                + "\t\t\tuseNioGlobalWorkerPool=true" + System.lineSeparator()
                + "\t\t}," + System.lineSeparator()
                + "\t\t" + System.lineSeparator()
                + "\t\tb=null" + System.lineSeparator()
                + "\t]," + System.lineSeparator()
                + "\tbackupGroupName=group1," + System.lineSeparator()
                + "\tscaleDownGroupName=null" + System.lineSeparator()
                + "]nodes=1 members=1";
        String result = ClusterConnectionControlHandler.formatTopology(topology);
        assertEquals(expResult, result);
    }
}
