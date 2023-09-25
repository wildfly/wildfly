/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import jakarta.ejb.Timer;

/**
 * @author Paul Ferraro
 */
public interface TimerRecorder {

    void record(Timer timer);
}
