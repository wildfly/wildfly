/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.remote;

import static org.jboss.as.test.clustering.InfinispanServerUtil.infinispanServerTestRule;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.infinispan.AbstractCacheTestCase;
import org.jboss.as.test.clustering.cluster.infinispan.bean.remote.RemoteCacheBean;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.ClassRule;
import org.junit.rules.TestRule;

/**
 * @author Paul Ferraro
 */
@ServerSetup(RemoteInfinispanCacheTestCase.InfinispanServerSetupTask.class)
public class RemoteInfinispanCacheTestCase extends AbstractCacheTestCase {

    @ClassRule
    public static final TestRule INFINISPAN_SERVER_RULE = infinispanServerTestRule();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return createDeployment(RemoteCacheBean.class, "org.infinispan.client.hotrod");
    }

    public static class InfinispanServerSetupTask extends ManagementServerSetupTask {
        public InfinispanServerSetupTask() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                        .startBatch()
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:add(port=%d,host=%s)", INFINISPAN_SERVER_PORT, INFINISPAN_SERVER_ADDRESS)
                            .add("/subsystem=infinispan/remote-cache-container=remote:add(default-remote-cluster=infinispan-server-cluster, marshaller=PROTOSTREAM, properties={infinispan.client.hotrod.auth_username=%s, infinispan.client.hotrod.auth_password=%s})", INFINISPAN_APPLICATION_USER, INFINISPAN_APPLICATION_PASSWORD)
                            .add("/subsystem=infinispan/remote-cache-container=remote/remote-cluster=infinispan-server-cluster:add(socket-bindings=[infinispan-server])")
                        .endBatch()
                        .build())
                    .tearDownScript(createScriptBuilder()
                        .startBatch()
                            .add("/subsystem=infinispan/remote-cache-container=remote:remove")
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:remove")
                        .endBatch()
                        .build())
                    .build());
        }
    }
}
