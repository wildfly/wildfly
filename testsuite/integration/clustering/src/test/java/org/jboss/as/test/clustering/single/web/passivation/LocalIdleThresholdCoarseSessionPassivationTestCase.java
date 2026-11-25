/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.web.passivation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Validates the correctness of session passivation events for a distributed session manager using a local,
 * passivating cache with time-based (idle-threshold) eviction and SESSION granularity.
 *
 * @author Radoslav Husar
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(StabilityServerSetupSnapshotRestoreTasks.Community.class)
public class LocalIdleThresholdCoarseSessionPassivationTestCase extends LocalIdleThresholdSessionPassivationTestCase {

    private static final String MODULE_NAME = LocalIdleThresholdCoarseSessionPassivationTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return getBaseDeployment(MODULE_NAME)
                .addAsWebInfResource(LocalMaxActiveSessionsSessionPassivationTestCase.class.getPackage(), "distributable-web-coarse-idle-threshold.xml", "distributable-web.xml")
                ;
    }
}
