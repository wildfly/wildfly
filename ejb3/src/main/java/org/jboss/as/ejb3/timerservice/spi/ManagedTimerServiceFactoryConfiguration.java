/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.spi;

/**
 * Enscapsulate the common configuration for a {@ManagedTimerServiceFactory}.
 * @author Paul Ferraro
 */
public interface ManagedTimerServiceFactoryConfiguration extends TimerServiceApplicableComponentConfiguration {

    /**
     * An invoker factory for the EJB component associated with this timer service
     * @return an invoker factory
     */
    TimedObjectInvokerFactory getInvokerFactory();
}
