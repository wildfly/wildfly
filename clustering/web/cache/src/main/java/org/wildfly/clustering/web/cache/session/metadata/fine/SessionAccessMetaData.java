/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;

/**
 * The volatile aspects of a session's meta-data.
 * @author Paul Ferraro
 */
public interface SessionAccessMetaData extends ImmutableSessionAccessMetaData {

    /**
     * Sets the last accessed duration (since this session was created) and last request duration.
     * @param sinceCreation the duration of time this session was created
     * @param lastAccessDuration the duration of time this session was last accessed
     */
    void setLastAccessDuration(Duration sinceCreation, Duration lastAccess);
}
