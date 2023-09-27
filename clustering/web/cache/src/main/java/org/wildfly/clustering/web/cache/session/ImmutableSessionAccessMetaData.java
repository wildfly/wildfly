/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;

/**
 * Immutable view of the volatile aspects of a session's meta-data.
 * @author Paul Ferraro
 */
public interface ImmutableSessionAccessMetaData {
    /**
     * Returns the duration of time between session creation and the start of the last access.
     * @return the duration of time between session creation and the start of the last access.
     */
    Duration getSinceCreationDuration();

    /**
     * Returns the duration of time between the start and of the last access.
     * @return the duration of time between the start and of the last access.
     */
    Duration getLastAccessDuration();
}
