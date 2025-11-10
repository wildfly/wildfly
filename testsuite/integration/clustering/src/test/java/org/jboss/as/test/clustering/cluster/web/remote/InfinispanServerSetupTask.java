/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_PASSWORD;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_USER;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_ADDRESS;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_PORT;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1_2_3;

import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * Server setup task that configures a hotrod client to connect to an Infinispan server.
 * @author Paul Ferraro
 */
public class InfinispanServerSetupTask extends CLIServerSetupTask {
    public InfinispanServerSetupTask() {
        this.builder.node(NODE_1_2_3.toArray(new String[0]))
                .setup("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:add(port=%d,host=%s)", INFINISPAN_SERVER_PORT, INFINISPAN_SERVER_ADDRESS)
                .setup("/subsystem=infinispan/remote-cache-container=web:add(default-remote-cluster=infinispan-server-cluster, tcp-keep-alive=true, marshaller=PROTOSTREAM, modules=[org.wildfly.clustering.web.hotrod], properties={infinispan.client.hotrod.auth_username=%s, infinispan.client.hotrod.auth_password=%s}, statistics-enabled=true)", INFINISPAN_APPLICATION_USER, INFINISPAN_APPLICATION_PASSWORD)
                .setup("/subsystem=infinispan/remote-cache-container=web/remote-cluster=infinispan-server-cluster:add(socket-bindings=[infinispan-server])")
                .teardown("/subsystem=infinispan/remote-cache-container=web:remove")
                .teardown("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:remove")
                ;
    }
}
