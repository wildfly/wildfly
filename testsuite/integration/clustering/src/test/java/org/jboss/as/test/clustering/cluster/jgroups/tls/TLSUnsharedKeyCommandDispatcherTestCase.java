/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import static org.junit.Assert.assertEquals;

import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.dispatcher.CommandDispatcherTestCase;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopology;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetriever;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;

/**
 * Variation of the standard {@link CommandDispatcherTestCase} that uses SSL/TLS-secured JGroups communication channel,
 * however, without a shared key. In this case, the nodes should reject the connection from each other and form two
 * singleton clusters.
 * <p>
 * This test prevents against a potential breakage in TLS secured transport wiring in JGroups subsystem.
 *
 * @author Radoslav Husar
 */
@ServerSetup({TLSServerSetupTasks.PhysicalKeyStoresServerSetupTask_NODE_1_2.class, TLSServerSetupTasks.UnsharedSecureJGroupsTransportServerSetupTask_NODE_1_2.class})
public class TLSUnsharedKeyCommandDispatcherTestCase extends CommandDispatcherTestCase {

    @Override
    public void test() throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ClusterTopologyRetriever bean = directory.lookupStateless(ClusterTopologyRetrieverBean.class, ClusterTopologyRetriever.class);

            ClusterTopology topology = bean.getClusterTopology();

            // These server should never cluster and end up with singleton clusters
            // Server log will contain a similar log – which is expected as keys are not preshared:
            // WARN  [org.jgroups.protocols.TCP] (TcpServer.Acceptor[7600]-1,null,null) JGRP000006: 127.0.0.1:7600: failed accepting connection from peer SSLSocket[....]: java.net.SocketException: Socket is closed
            assertEquals("TLS/SSL-secured cluster nodes formed a cluster while they shouldn't have since they did not have a pre-shared key", 1, topology.getNodes().size());
        }
    }

    @Override
    public void legacy() throws Exception {
        // This test variant is redundant
    }
}
