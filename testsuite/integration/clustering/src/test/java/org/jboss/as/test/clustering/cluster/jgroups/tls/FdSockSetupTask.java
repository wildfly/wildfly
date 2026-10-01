/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;

import org.jboss.as.test.shared.ManagementServerSetupTask;

/**
 * {@link org.jboss.as.arquillian.api.ServerSetupTask}s that replaces the FD_SOCK2 JGroups layer by FD_SOCK JGroups layer.
 */
public class FdSockSetupTask extends ManagementServerSetupTask {
    public FdSockSetupTask() {
        super(createContainerSetConfigurationBuilder()
                .addContainers(NODE_1_2, createContainerConfigurationBuilder()
                        .setupScript(createScriptBuilder()
                                .add("/subsystem=jgroups/stack=tcp/protocol=FD_SOCK2:remove")
                                .add("/subsystem=jgroups/stack=tcp/protocol=FD_SOCK:add(socket-binding=jgroups-tcp-fd, add-index=3)")
                                .build())
                        .build())
                .build());
    }
}
