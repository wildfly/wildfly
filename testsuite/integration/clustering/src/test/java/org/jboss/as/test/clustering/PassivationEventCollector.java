/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import java.time.Instant;
import java.util.Deque;

/**
 * @author Paul Ferraro
 */
public interface PassivationEventCollector {
    /**
     * Returns a queue of passivation timestamps in order of receipt.
     * @return a queue of passivation timestamps in order of receipt.
     */
    Deque<Instant> getPassivations();

    /**
     * Returns a queue of activation timestamps in order of receipt.
     * @return a queue of activation timestamps in order of receipt.
     */
    Deque<Instant> getActivations();
}
