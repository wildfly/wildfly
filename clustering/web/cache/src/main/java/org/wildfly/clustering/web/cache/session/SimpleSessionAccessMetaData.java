/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author Paul Ferraro
 */
public class SimpleSessionAccessMetaData implements SessionAccessMetaData {

    private volatile Duration sinceCreation = Duration.ZERO;
    private volatile Duration lastAccess = Duration.ZERO;

    @Override
    public Duration getSinceCreationDuration() {
        return this.sinceCreation;
    }

    @Override
    public Duration getLastAccessDuration() {
        return this.lastAccess;
    }

    @Override
    public void setLastAccessDuration(Duration sinceCreation, Duration lastAccess) {
        this.sinceCreation = normalize(sinceCreation);
        this.lastAccess = normalize(lastAccess);
    }

    private static Duration normalize(Duration duration) {
        // Only retain millisecond precision, rounding up to nearest millisecond
        Duration truncatedDuration = duration.truncatedTo(ChronoUnit.MILLIS);
        return !duration.equals(truncatedDuration) ? truncatedDuration.plusMillis(1) : duration;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append(" { ");
        builder.append("since-creation = ").append(this.sinceCreation);
        builder.append(", last-access = ").append(this.lastAccess);
        return builder.append("}").toString();
    }
}
