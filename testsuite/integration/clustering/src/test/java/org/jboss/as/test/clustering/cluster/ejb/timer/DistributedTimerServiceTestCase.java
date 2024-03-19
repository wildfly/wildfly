/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.shared.IntermittentFailure;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.BeforeClass;

/**
 * @author Paul Ferraro
 */
public class DistributedTimerServiceTestCase extends AbstractTimerServiceTestCase {
    @BeforeClass
    public static void beforeClass() {
        IntermittentFailure.thisTestIsFailingIntermittently("WFLY-19139");
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createArchive();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createArchive();
    }

    private static Archive<?> createArchive() {
        return createArchive(DistributedTimerServiceTestCase.class);
    }
}
