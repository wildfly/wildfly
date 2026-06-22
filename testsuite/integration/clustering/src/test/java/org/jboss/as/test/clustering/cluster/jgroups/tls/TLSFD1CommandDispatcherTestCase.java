/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.cluster.dispatcher.CommandDispatcherTestCase;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetrieverBean;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * Variant of the {@link CommandDispatcherTestCase} with TLS-secured TCP transport protocol.
 *
 * Use FD_SOCK1, warning should not be printed
 *
 * @author Radoslav Husar
 */
@ServerSetup({
        FdSockSetupTask.class,
        TLSServerSetupTask.PerNodeKeyStore_NODE_1_2.class,
        TLSServerSetupTask.PerNodeSecureJGroupsTransport_TCP_NODE_1_2.class,
})
class TLSFD1CommandDispatcherTestCase extends CommandDispatcherTestCase {

    @ArquillianResource
    @TargetsContainer(NODE_1)
    private ManagementClient client;

    @Override
    @Test
    public void test() throws Exception {
        Assert.assertFalse("WFLYCLJG0037 found", JGroupsLogsUtil.findWFLYCLJG0037(client));

        // original test
        this.test(ClusterTopologyRetrieverBean.class);
    }

    @Override
    @Test
    public void legacy() throws Exception {
        // This test variant is redundant
    }

}
