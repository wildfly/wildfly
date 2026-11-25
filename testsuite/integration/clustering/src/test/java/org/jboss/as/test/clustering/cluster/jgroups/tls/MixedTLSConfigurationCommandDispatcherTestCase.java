/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.dispatcher.CommandDispatcherTestCase;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopology;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetriever;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.junit.jupiter.api.Test;

/**
 * Variation of the standard {@link CommandDispatcherTestCase} that uses SSL/TLS-secured JGroups communication channel,
 * however, TLS is configured on one node only. In this case, the nodes should reject the connection from each other and form two
 * singleton clusters.
 * <p>
 * This test prevents against a potential breakage in TLS secured transport wiring in JGroups subsystem.
 *
 * @author Radoslav Husar
 */
@ServerSetup({
        TLSServerSetupTask.PerNodeKeyStore_NODE_1_2.class,
        TLSServerSetupTask.PerNodeSecureJGroupsTransport_TCP_NODE_1.class,
})
class MixedTLSConfigurationCommandDispatcherTestCase extends CommandDispatcherTestCase {

    @Override
    @Test
    public void test() throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ClusterTopologyRetriever bean = directory.lookupStateless(ClusterTopologyRetrieverBean.class, ClusterTopologyRetriever.class);

            ClusterTopology topology = bean.getClusterTopology();

            // These server should never cluster and end up with singleton clusters
            assertEquals(1, topology.getNodes().size(), "TLS/SSL-secured cluster nodes formed a cluster while they shouldn't have since one node doesn't have configured security");
        }
    }

    @Override
    @Test
    public void legacy() throws Exception {
        // This test variant is redundant
    }
}
