/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.setuptasks;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.observability.collector.InMemoryCollector;

public class InMemoryCollectorSetupTask implements ServerSetupTask {
    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        InMemoryCollector.getInstance().start();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        InMemoryCollector.getInstance().shutdown();
    }
}
