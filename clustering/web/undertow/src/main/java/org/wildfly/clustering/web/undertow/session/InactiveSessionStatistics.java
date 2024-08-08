/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.time.Duration;

/**
 * Statistics for inactive sessions.
 * @author Paul Ferraro
 */
public interface InactiveSessionStatistics {

    /**
     * @return The number of expired sessions
     */
    long getExpiredSessionCount();

    /**
     * @return The longest a session has been alive, as a time duration
     */
    Duration getMaxSessionLifetime();

    /**
     * @return The average session lifetime, as a time duration
     */
    Duration getMeanSessionLifetime();
}
