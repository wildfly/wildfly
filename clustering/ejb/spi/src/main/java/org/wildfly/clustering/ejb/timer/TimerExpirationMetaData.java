/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.time.Instant;

/**
 * Describes the expiration-related metadata of a timer.
 * @author Paul Ferraro
 */
public interface TimerExpirationMetaData {

    Instant getNextTimeout();
}
