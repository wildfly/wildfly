/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import jakarta.ejb.TimerConfig;

/**
 * @author Paul Ferraro
 */
public class AbstractIntervalTimerBean extends AbstractManualTimerBean {

    public AbstractIntervalTimerBean(boolean persistent) {
        super(service -> service.createIntervalTimer(1000, 1000, new TimerConfig("interval", persistent)));
    }
}
