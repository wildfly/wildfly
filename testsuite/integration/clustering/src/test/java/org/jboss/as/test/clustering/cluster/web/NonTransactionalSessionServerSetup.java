/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1_2_3;

import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * @author Paul Ferraro
 */
public class NonTransactionalSessionServerSetup extends CLIServerSetupTask {
    public NonTransactionalSessionServerSetup() {
        this.builder.node(NODE_1_2_3.toArray(new String[0]))
                .setup("/subsystem=infinispan/cache-container=web/distributed-cache=concurrent:add()")
                .setup("/subsystem=infinispan/cache-container=web/distributed-cache=concurrent/store=file:add(passivation=true, purge=true)")
                .teardown("/subsystem=infinispan/cache-container=web/distributed-cache=concurrent:remove()")
        ;
    }
}
