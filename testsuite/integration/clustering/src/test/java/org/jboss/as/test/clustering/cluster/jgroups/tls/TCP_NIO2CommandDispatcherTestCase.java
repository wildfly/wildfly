/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.dispatcher.CommandDispatcherTestCase;
import org.jboss.as.test.clustering.cluster.jgroups.TCP_NIO2ServerSetupTask;
import org.junit.jupiter.api.Test;

/**
 * Variant of the {@link CommandDispatcherTestCase} with the TCP_NIO2 transport protocol.
 *
 * @author Radoslav Husar
 */
@ServerSetup(TCP_NIO2ServerSetupTask.class)
class TCP_NIO2CommandDispatcherTestCase extends CommandDispatcherTestCase {

    @Override
    @Test
    public void legacy() throws Exception {
        // This test variant is redundant
    }

}
