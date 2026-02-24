/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.web.passivation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Validates the correctness of session passivation events for a distributed session manager using a local,
 * passivating cache with time-based (idle-threshold) eviction and SESSION granularity.
 * Also, verifies functionality of community namespace, i.e. xmlns="urn:jboss:distributable-web:community:5.0"
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup({
        SnapshotRestoreSetupTask.class, // MUST be first!
        StabilityServerSetupSnapshotRestoreTasks.Community.class,
})
public class LocalIdleThresholdCoarseSessionPassivationTestCase extends LocalIdleThresholdSessionPassivationTestCase {

    private static final String MODULE_NAME = LocalIdleThresholdCoarseSessionPassivationTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return getBaseDeployment(MODULE_NAME)
                .addAsWebInfResource(LocalMaxActiveSessionsSessionPassivationTestCase.class.getPackage(), "distributable-web-coarse-idle-threshold.xml", "distributable-web.xml")
                ;
    }
}
