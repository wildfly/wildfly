/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.web.passivation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Validates the correctness of session passivation events for a distributed session manager using a local,
 * passivating cache with time-based (idle-threshold) eviction with the default session manager.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup({
        SnapshotRestoreSetupTask.class, // MUST be first!
        StabilityServerSetupSnapshotRestoreTasks.Community.class,
        LocalIdleThresholdDefaultSessionManagerSessionPassivationTestCase.ServerSetupTask.class,
})
public class LocalIdleThresholdDefaultSessionManagerSessionPassivationTestCase extends LocalIdleThresholdSessionPassivationTestCase {

    static class ServerSetupTask extends ManagementServerSetupTask {
        ServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=distributable-web/infinispan-session-management=default:write-attribute(name=idle-threshold, value=PT1S)")
                            .endBatch()
                            .build())
                    .build());
        }
    }

    private static final String MODULE_NAME = LocalIdleThresholdDefaultSessionManagerSessionPassivationTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        // n.b. do not add jboss-web.xml no distributable-web.xml because that would OVERRIDE the default session manager configuration
        // that this test is intended to test!
        return getBaseDeployment(MODULE_NAME);
    }
}
