/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.timer.passivation.bean;

import java.time.Duration;

import org.jboss.as.test.clustering.PassivationEventTracker;

/**
 * Remote interface for a singleton bean that manages timers.
 * TimerInfo objects are created and managed entirely on the server side to ensure that serialization tracking only measures timer passivation, not EJB remoting serialization.
 *
 * @author Radoslav Husar
 */
public interface TimerTracker extends PassivationEventTracker {

    /**
     * Creates a transient or persistent timer with a TimerInfo.
     * The TimerInfo is created on the server and never sent to the client, ensuring that serialization only occurs during timer passivation.
     *
     * @param name the name for the timer
     * @param persistent whether the timer is persistent
     * @param duration the duration in milliseconds before the timer expires
     */
    void createTimer(String name, boolean persistent, Duration duration);
}
