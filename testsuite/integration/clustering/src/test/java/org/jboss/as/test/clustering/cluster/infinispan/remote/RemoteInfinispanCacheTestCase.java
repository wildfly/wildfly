/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.infinispan.remote;

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
