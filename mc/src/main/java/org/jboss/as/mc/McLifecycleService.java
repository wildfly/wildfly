/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @param <T> the MC bean type
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class McLifecycleService<T> implements Service<T> {
    private final InjectedValue<T> value = new InjectedValue<T>();
    private final Method startMethod;
    private final Method stopMethod;

    /**
     * Construct a new instance.
     *
     * @param startMethod the start method or {@code null} for none
     * @param stopMethod the stop method or {@code null} for none
     */
    public McLifecycleService(final Method startMethod, final Method stopMethod) {
        this.startMethod = startMethod;
        this.stopMethod = stopMethod;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        final Method method = startMethod;
        if (method != null) try {
            method.invoke(value.getValue());
        } catch (IllegalAccessException e) {
            throw new StartException("Failed to access method " + method.getName(), e);
        } catch (InvocationTargetException e) {
            throw new StartException("Failed to invoke method " + method.getName(), e.getCause());
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        final Method method = stopMethod;
        if (method != null) try {
            method.invoke(value.getValue());
        } catch (IllegalAccessException e) {
            // todo log it
        } catch (InvocationTargetException e) {
            // todo log it
        }
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException, IllegalArgumentException {
        return value.getValue();
    }

    /**
     * Get the value injector.
     *
     * @return the value injector
     */
    public Injector<T> getValueInjector() {
        return value;
    }
}
