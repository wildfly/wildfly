/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.cluster.dispatcher.CommandDispatcherTestCase;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetrieverBean;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * Variant of the {@link CommandDispatcherTestCase} with TLS-secured TCP transport protocol.
 *
 * Use FD_SOCK2, warning should be printed
 *
 * @author Radoslav Husar
 */
@ServerSetup({
        TLSServerSetupTask.PerNodeKeyStore_NODE_1_2.class,
        TLSServerSetupTask.PerNodeSecureJGroupsTransport_TCP_NODE_1_2.class,
})
class TLSFD2CommandDispatcherTestCase extends CommandDispatcherTestCase {

    @ArquillianResource
    @TargetsContainer(NODE_1)
    private ManagementClient client;

    @Override
    @Test
    public void test() throws Exception {
        // /subsystem=logging/log-file=server.log:read-log-file
        ModelNode operation = new ModelNode();
        operation.get("address").add("subsystem", "logging").add("log-file", "server.log");
        operation.get("operation").set("read-log-file");
        operation.get("lines").set(150);
        operation.get("tail").set(true);
        ModelNode response = client.getControllerClient().execute(operation);
        Assert.assertTrue("Unable to read logs", "success".equals(response.get("outcome").asString()));
        Assert.assertTrue("WFLYCLJG0037 not found", response.get("result").asList().stream().anyMatch(lineNode -> lineNode.asString().contains("WFLYCLJG0037")));

        // original test
        this.test(ClusterTopologyRetrieverBean.class);
    }

    @Override
    @Test
    public void legacy() throws Exception {
        // This test variant is redundant
    }

}
