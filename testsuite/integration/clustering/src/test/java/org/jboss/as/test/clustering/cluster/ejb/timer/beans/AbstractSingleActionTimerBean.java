/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import jakarta.ejb.TimerConfig;

/**
 * @author Paul Ferraro
 */
public class AbstractSingleActionTimerBean extends AbstractManualTimerBean {

    public AbstractSingleActionTimerBean(boolean persistent) {
        super(service -> service.createSingleActionTimer(1800, new TimerConfig("single", persistent)));
    }
}
