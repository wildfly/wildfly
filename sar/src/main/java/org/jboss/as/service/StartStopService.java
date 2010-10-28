/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.service;

import java.lang.reflect.Method;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

/**
 * Service wrapper for legacy JBoss services that controls the service start and stop lifecycle.
 *
 * @author John E. Bailey
 */
public class StartStopService<T> implements Service<T> {
    private static final Logger log = Logger.getLogger("org.jboss.as.service");
    private final Value<T> serviceValue;

    /**
     * Construct new instance.
     *
     * @param serviceValue The service value
     */
    public StartStopService(Value<T> serviceValue) {
        this.serviceValue = serviceValue;
    }

    /** {@inheritDoc} */
    public void start(StartContext context) throws StartException {
        final T service = getValue();
        // Handle Start
        log.debugf("Starting Service: %s", context.getController().getName());
        try {
            final Method startMethod = service.getClass().getMethod("start");
            ClassLoader old = SecurityActions.setThreadContextClassLoader(service.getClass().getClassLoader());
            try {
                startMethod.invoke(service);
            } finally {
                SecurityActions.resetThreadContextClassLoader(old);
            }
        } catch(NoSuchMethodException e) {
        } catch(Exception e) {
            throw new StartException("Failed to execute legacy service start", e);
        }
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
        final T service = getValue();
        // Handle Stop
        log.debugf("Stopping Service: %s", context.getController().getName());
        try {
            Method stopMethod = service.getClass().getMethod("stop");
            ClassLoader old = SecurityActions.setThreadContextClassLoader(service.getClass().getClassLoader());
            try {
                stopMethod.invoke(service);
            } finally {
                SecurityActions.resetThreadContextClassLoader(old);
            }
        } catch(NoSuchMethodException e) {
        }  catch(Exception e) {
            log.error("Failed to execute legacy service stop", e);
        }
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        return serviceValue.getValue();
    }
}
