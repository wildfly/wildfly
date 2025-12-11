/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.dispatcher.CommandDispatcherTestCase;
import org.jboss.as.test.clustering.cluster.jgroups.TCP_NIO2ServerSetupTask;

/**
 * Variant of the {@link CommandDispatcherTestCase} with TLS-secured TCP_NIO2 transport protocol.
 *
 * @author Radoslav Husar
 */
@ServerSetup({TCP_NIO2ServerSetupTask.class, TLSServerSetupTasks.SharedStoreSecureJGroupsTCP_NIO2TransportServerSetupTask_NODE_1_2.class})
public class TLSTCP_NIO2CommandDispatcherTestCase extends CommandDispatcherTestCase {

    @Override
    public void legacy() throws Exception {
        // This test variant is redundant
    }

}
