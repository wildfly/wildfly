/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.sso.remote;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_PASSWORD;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_USER;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_ADDRESS;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_PORT;

import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * @author Paul Ferraro
 */
public class InfinispanServerSetupTask extends CLIServerSetupTask {
    public InfinispanServerSetupTask() {
        this.builder.node(AbstractClusteringTestCase.NODE_1_2.toArray(new String[0]))
                .setup("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:add(port=%d,host=%s)", INFINISPAN_SERVER_PORT, INFINISPAN_SERVER_ADDRESS)
                .setup("/subsystem=infinispan/remote-cache-container=sso:add(default-remote-cluster=infinispan-server-cluster, marshaller=PROTOSTREAM, modules=[org.wildfly.clustering.web.hotrod], properties={infinispan.client.hotrod.auth_username=%s, infinispan.client.hotrod.auth_password=%s})", INFINISPAN_APPLICATION_USER, INFINISPAN_APPLICATION_PASSWORD)
                .setup("/subsystem=infinispan/remote-cache-container=sso/remote-cluster=infinispan-server-cluster:add(socket-bindings=[infinispan-server])")
                .teardown("/subsystem=infinispan/remote-cache-container=sso:remove")
                .teardown("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:remove")
                ;
    }
}
