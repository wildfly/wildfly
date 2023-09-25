/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * @author Paul Ferraro
 */
public class LocalRoutingServerSetup extends CLIServerSetupTask {

    public LocalRoutingServerSetup() {
        this.builder.node(AbstractClusteringTestCase.THREE_NODES)
                .setup("/subsystem=distributable-web/routing=infinispan:remove")
                .setup("/subsystem=distributable-web/routing=local:add")
                .teardown("/subsystem=distributable-web/routing=local:remove")
                .teardown("/subsystem=distributable-web/routing=infinispan:add(cache-container=web, cache=routing)")
        ;
    }

}
