/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_PASSWORD;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_USER;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_ADDRESS;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_PORT;

import org.jboss.as.test.shared.ManagementServerSetupTask;

/**
 * @author Radoslav Husar
 */
public class ServerSetupTask extends ManagementServerSetupTask {
    public ServerSetupTask() {
        super("default", createContainerConfigurationBuilder()
                .setupScript(createScriptBuilder()
                        .startBatch()
                        .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:add(port=%d, host=%s)", INFINISPAN_SERVER_PORT, INFINISPAN_SERVER_ADDRESS)
                        .add("/subsystem=infinispan/remote-cache-container=query:add(default-remote-cluster=infinispan-server-cluster, tcp-keep-alive=true, marshaller=PROTOSTREAM, properties={infinispan.client.hotrod.auth_username=%s, infinispan.client.hotrod.auth_password=%s}, statistics-enabled=true)", INFINISPAN_APPLICATION_USER, INFINISPAN_APPLICATION_PASSWORD)
                        .add("/subsystem=infinispan/remote-cache-container=query/remote-cluster=infinispan-server-cluster:add(socket-bindings=[infinispan-server])")
                        .endBatch()
                        .build()
                )
                .tearDownScript(createScriptBuilder()
                        .startBatch()
                        .add("/subsystem=infinispan/remote-cache-container=query:remove")
                        .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:remove")
                        .endBatch()
                        .build())
                .build()
        );
    }
}