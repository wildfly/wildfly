/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.spi;

/**
 * Configuration for a TimerService-aware deployment.
 * @author Paul Ferraro
 */
public interface TimerServiceApplicableComponentConfiguration {

    /**
     * A registry of timer services associated with the same deployment.
     * @return a timer service registry
     */
    TimerServiceRegistry getTimerServiceRegistry();

    /**
     * A registrar for timers created by this timer service
     * @return a timer registrar
     */
    TimerListener getTimerListener();
}
