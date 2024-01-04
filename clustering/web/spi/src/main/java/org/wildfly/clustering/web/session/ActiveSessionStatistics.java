/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.session;

/**
 * Statistics for active sessions.
 * @author Paul Ferraro
 */
public interface ActiveSessionStatistics {

    /**
     * @return The number of active sessions
     */
    long getActiveSessionCount();
}
