/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ee.cache.offset.Value;

/**
 * Encapsulates the mutable values of the session metadata.
 * @author Paul Ferraro
 */
public interface MutableSessionMetaDataValues {
    Value<Duration> getTimeout();

    Value<Instant> getLastAccessStartTime();

    Value<Instant> getLastAccessEndTime();
}
