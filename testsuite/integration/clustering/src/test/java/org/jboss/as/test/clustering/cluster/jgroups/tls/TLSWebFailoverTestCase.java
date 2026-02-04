/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.web.CoarseWebFailoverTestCase;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Variation of the standard {@link CoarseWebFailoverTestCase} that uses TLS/SSL-secured JGroups communication channel.
 *
 * @author Radoslav Husar
 */
@ServerSetup({
        StabilityServerSetupSnapshotRestoreTasks.Community.class,
        TLSServerSetupTasks.SharedPhysicalKeyStoresServerSetupTask.class,
        TLSServerSetupTasks.SharedStoreSecureJGroupsTransportServerSetupTask_NODE_1_2_3.class,
})
public class TLSWebFailoverTestCase extends CoarseWebFailoverTestCase {

    // No changes to the tests are necessary, securing the channel is transparent to the underlying test.

}
