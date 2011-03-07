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

package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.msc.value.Value;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Resource injection capable of executing the resource injection using a Method instance.
 *
 * @author John E. Bailey
 */
public class MethodResourceInjection extends AbstractResourceInjection {
    private final Method method;

    /**
     * Construct an instance.
     *
     * @param method The method value to use for injection
     * @param value The injection value
     */
    public MethodResourceInjection(final Method method, final Value<ManagedReferenceFactory> value) {
        super(value, method.getParameterTypes()[0].isPrimitive());
        this.method = method;
    }

    /** {@inheritDoc} */
    protected void doInject(final Object target, final Object value) {
        try {
            method.invoke(target, value);
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException re) {
                throw re;
            } catch (Error er) {
                throw er;
            } catch (Throwable throwable) {
                throw new UndeclaredThrowableException(throwable, e.getMessage());
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }
}
