/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.util;

import org.jboss.as.ejb3.EjbMessages;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.value.Value;

/**
 * Represents a lookup of a service at runtime, used in place of optional dependencies to prevent race conditions
 *
 * @author Stuart Douglas
 */
public class ServiceLookupValue<T> implements Value<T> {

    private final ServiceRegistry serviceContainer;
    private final ServiceName serviceName;

    public ServiceLookupValue(final ServiceRegistry serviceContainer, final ServiceName serviceName) {
        this.serviceContainer = serviceContainer;
        this.serviceName = serviceName;
    }

    /**
     *
     * @return The value of the service
     * @throws IllegalStateException If the service it not available or is not up
     */
    @Override
    public T getValue() throws IllegalStateException {
        final ServiceController<?> controller = serviceContainer.getService(serviceName);
        if(controller == null) {
            throw EjbMessages.MESSAGES.serviceNotFound(serviceName);
        }
        return (T) controller.getValue();
    }

    /**
     *
     * @return The value of the service, or null if the service is not present
     */
    public T getOptionalValue() {
        final ServiceController<?> controller = serviceContainer.getService(serviceName);
        if(controller != null) {
            return (T) controller.getValue();
        }
        return null;
    }
}
