/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.ejb.timer.passivation.bean;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;

import org.jboss.as.test.clustering.PassivationEventCollector;

/**
 * @author Paul Ferraro
 */
public interface PassivationTimeoutCollector extends PassivationEventCollector {
    Duration TIMEOUT_INTERVAL = Duration.ofSeconds(2L);

    Deque<Instant> getTimeouts();
}
