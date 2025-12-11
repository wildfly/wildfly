/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1_2;

import org.jboss.as.test.shared.ManagementServerSetupTask;

/**
 * {@link org.jboss.as.arquillian.api.ServerSetupTask}s that replaces a TCP transport with TCP_NIO2.
 *
 * @author Radoslav Husar
 */
public class TCP_NIO2ServerSetupTask extends ManagementServerSetupTask {
    public TCP_NIO2ServerSetupTask() {
        super(createContainerSetConfigurationBuilder()
                .addContainers(NODE_1_2, createContainerConfigurationBuilder()
                        .setupScript(createScriptBuilder()
                                .startBatch()
                                .add("/subsystem=jgroups/stack=tcp/transport=TCP:remove")
                                .add("/subsystem=jgroups/stack=tcp/transport=TCP_NIO2:add(socket-binding=jgroups-tcp)")
                                .endBatch()
                                .build())
                        .tearDownScript(createScriptBuilder()
                                .startBatch()
                                .add("/subsystem=jgroups/stack=tcp/transport=TCP_NIO2:remove")
                                .add("/subsystem=jgroups/stack=tcp/transport=TCP:add(socket-binding=jgroups-tcp)")
                                .endBatch()
                                .build())
                        .build())
                .build());
    }
}
