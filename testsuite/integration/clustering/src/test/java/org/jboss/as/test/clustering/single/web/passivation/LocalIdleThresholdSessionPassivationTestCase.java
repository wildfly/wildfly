/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.web.passivation;

import java.time.Duration;

/**
 * Validates the correctness of session activation/passivation events for a distributed session manager using a local,
 * passivating cache with time-based (idle-threshold) eviction.
 *
 * @author Radoslav Husar
 */
public abstract class LocalIdleThresholdSessionPassivationTestCase extends LocalSessionPassivationTestCase {
    // As specified in distributable-web-*-idle-threshold.xml
    static final Duration IDLE_THRESHOLD = Duration.ofSeconds(1L);

    public LocalIdleThresholdSessionPassivationTestCase() {
        super(IDLE_THRESHOLD);
    }
}
