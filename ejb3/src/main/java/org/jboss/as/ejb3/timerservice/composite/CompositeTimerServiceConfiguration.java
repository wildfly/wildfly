/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.composite;

import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceConfiguration;

/**
 * @author Paul Ferraro
 */
public interface CompositeTimerServiceConfiguration extends ManagedTimerServiceConfiguration {

    ManagedTimerService getTransientTimerService();
    ManagedTimerService getPersistentTimerService();
}
