/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

/**
 * @author Paul Ferraro
 */
public interface ManualTimerBean extends TimerBean {

    void createTimer();

    void cancel();
}
