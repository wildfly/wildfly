/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.spi;

import org.jboss.as.ejb3.component.EJBComponent;

/**
 * @author Paul Ferraro
 */
public interface ManagedTimerServiceFactory {

    /**
     * Creates a managed timer service for the specified component.
     * @param component an EJB component
     * @return a managed timer service.
     */
    ManagedTimerService createTimerService(EJBComponent component);
}
