/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Paul Ferraro
 */
public interface TimeoutMetaData {
    /**
     * Returns the time of the next timeout event, or null if there are no future timeout events.
     * @return the optional time of the next timeout event
     */
    Optional<Instant> getNextTimeout();
}
