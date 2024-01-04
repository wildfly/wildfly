/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.ManagementServerSetupTask;

/**
 * @author Paul Ferraro
 */
public class LocalRoutingServerSetup extends ManagementServerSetupTask {

    public LocalRoutingServerSetup() {
        super(AbstractClusteringTestCase.NODE_1_2_3, createContainerConfigurationBuilder()
                .setupScript(createScriptBuilder()
                        .startBatch()
                            .add("/subsystem=distributable-web/routing=infinispan:remove")
                            .add("/subsystem=distributable-web/routing=local:add")
                        .endBatch()
                        .build())
                .tearDownScript(createScriptBuilder()
                        .startBatch()
                            .add("/subsystem=distributable-web/routing=local:remove")
                            .add("/subsystem=distributable-web/routing=infinispan:add(cache-container=web, cache=routing)")
                        .endBatch()
                        .build())
                .build());
    }
}
