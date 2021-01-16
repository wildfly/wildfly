/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.messaging.ha;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Assume;
import org.junit.BeforeClass;

/**
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class ReplicatedFailoverTestCase extends FailoverTestCase {
    private static final ModelNode MASTER_STORE_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/ha-policy=replication-master").toModelNode();
    private static final ModelNode SLAVE_STORE_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/ha-policy=replication-slave").toModelNode();

    @BeforeClass
    public static void conditionallyIgnore() throws Exception {
        // https://issues.redhat.com/browse/WFLY-14071
        // Reenable once we have a release of Artemis with https://issues.redhat.com/browse/ENTMQBR-2925
        Assume.assumeFalse("true".equals(System.getenv().get("GITHUB_ACTIONS")));
    }

    @Override
    protected void setUpServer1(ModelControllerClient client) throws Exception {
        configureCluster(client);

        // /subsystem=messaging-activemq/server=default/ha-policy=replication-master:add(cluster-name=my-cluster, check-for-live-server=true)
        ModelNode operation = Operations.createAddOperation(MASTER_STORE_ADDRESS);
        operation.get("cluster-name").set("my-cluster");
        operation.get("check-for-live-server").set(true);
        execute(client, operation);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
//        jmsOperations.enableMessagingTraces();
    }

    @Override
    protected void setUpServer2(ModelControllerClient client) throws Exception {
        configureCluster(client);

        // /subsystem=messaging-activemq/server=default/ha-policy=replication-slave:add(cluster-name=my-cluster, restart-backup=true)
        ModelNode operation = Operations.createAddOperation(SLAVE_STORE_ADDRESS);
        operation.get("cluster-name").set("my-cluster");
        operation.get("restart-backup").set(true);
        execute(client, operation);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
//        jmsOperations.enableMessagingTraces();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // leave some time after servers are setup and reloaded so that the cluster is formed
        Thread.sleep(TimeoutUtil.adjust(2000));
    }

    private void configureCluster(ModelControllerClient client) throws Exception {
        // /subsystem=messaging-activemq/server=default:write-attribute(name=cluster-user, value=clusteruser)
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("cluster-password");
        operation.get(VALUE).set("clusterpassword");
        execute(client, operation);

        // /subsystem=messaging-activemq/server=default:write-attribute(name=cluster-password, value=clusterpwd)
        operation = new ModelNode();
        operation.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("cluster-user");
        operation.get(VALUE).set("clusteruser");
        execute(client, operation);
    }

    @Override
    protected void testMasterInSyncWithReplica(ModelControllerClient client) throws Exception {
        ModelNode operation = Operations.createReadAttributeOperation(
                PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/ha-policy=replication-master").toModelNode(),
                "synchronized-with-backup");
        boolean synced = false;
        long start = System.currentTimeMillis();
        while (!synced && (System.currentTimeMillis() - start < TimeoutUtil.adjust(10000))) {
            synced = execute(client, operation).asBoolean();
        }
        assertTrue(synced);
    }

    @Override
    protected void testSlaveInSyncWithReplica(ModelControllerClient client) throws Exception {
        ModelNode operation = Operations.createReadAttributeOperation(
                PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/ha-policy=replication-slave").toModelNode(),
                "synchronized-with-live");
        assertTrue(execute(client, operation).asBoolean());
    }

    @Override
    protected void testMasterOutOfSyncWithReplica(ModelControllerClient client) throws Exception {
        ModelNode operation = Operations.createReadAttributeOperation(
                PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/ha-policy=replication-master").toModelNode(),
                "synchronized-with-backup");
        boolean synced = false;
        long start = System.currentTimeMillis();
        while (!synced && (System.currentTimeMillis() - start < TimeoutUtil.adjust(10000))) {
            synced = execute(client, operation).asBoolean();
        }
        assertFalse(synced);
    }

    @Override
    protected void testSlaveOutOfSyncWithReplica(ModelControllerClient client) throws Exception {
        ModelNode operation = Operations.createReadAttributeOperation(
                PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/ha-policy=replication-slave").toModelNode(),
                "synchronized-with-live");
        assertFalse(execute(client, operation).asBoolean());
    }
}
