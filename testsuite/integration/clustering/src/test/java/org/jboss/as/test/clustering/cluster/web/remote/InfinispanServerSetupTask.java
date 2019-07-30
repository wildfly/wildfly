/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.web.remote;

import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * @author Paul Ferraro
 */
public class InfinispanServerSetupTask extends CLIServerSetupTask {
    public InfinispanServerSetupTask() {
        this.builder.node(AbstractClusteringTestCase.THREE_NODES)
                .setup("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:add(port=11622,host=%s)", AbstractClusteringTestCase.TESTSUITE_NODE0)
                .setup("/subsystem=infinispan/remote-cache-container=web:add(default-remote-cluster=infinispan-server-cluster, module=org.wildfly.clustering.web.hotrod)")
                .setup("/subsystem=infinispan/remote-cache-container=web/remote-cluster=infinispan-server-cluster:add(socket-bindings=[infinispan-server])")
                .setup("/subsystem=infinispan/remote-cache-container=web/near-cache=invalidation:add()")
                .teardown("/subsystem=infinispan/remote-cache-container=web:remove")
                .teardown("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server-1:remove")
                ;
    }
}
