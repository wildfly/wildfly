/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import java.util.Timer;
import java.util.concurrent.ExecutorService;

import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceConfiguration;

/**
 * @author Paul Ferraro
 */
public interface TimerServiceConfiguration extends ManagedTimerServiceConfiguration {

    ExecutorService getExecutor();

    Timer getTimer();

    TimerPersistence getTimerPersistence();
}
