/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.spi;

import org.jboss.as.ejb3.component.EJBComponent;

/**
 * Factory for creating a {@link TimedObjectInvoker} for an EJB component.
 * @author Paul Ferraro
 */
public interface TimedObjectInvokerFactory {

    /**
     * Creates an invoker for the specified EJB component
     * @param component an EJB component
     * @return an invoker
     */
    TimedObjectInvoker createInvoker(EJBComponent component);
}
