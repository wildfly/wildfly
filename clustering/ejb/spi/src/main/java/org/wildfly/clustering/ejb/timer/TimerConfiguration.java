/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.time.Instant;

/**
 * Encapsulates the configuration of a timer.
 * @author Paul Ferraro
 */
public interface TimerConfiguration {

    Instant getStart();
}
