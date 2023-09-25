/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.sso;

import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * @author Paul Ferraro
 */
public class HostSSOServerSetupTask extends CLIServerSetupTask {
    public HostSSOServerSetupTask() {
        this.builder.node(AbstractClusteringTestCase.TWO_NODES)
                .setup("/subsystem=undertow/server=default-server/host=default-host/setting=single-sign-on:add")
                .teardown("/subsystem=undertow/server=default-server/host=default-host/setting=single-sign-on:remove")
        ;
    }
}