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

package org.jboss.as.deployment.managedbean;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.value.Values;

import java.lang.reflect.Method;

/**
 * Resource injection capable of executing the resource injection using a Method instance.
 *
 * @author John E. Bailey
 */
public class MethodResourceInjection<T> extends ResourceInjection<T> {
    private final String methodName;
    private final String injectedTypeName;

    /**
     * Construct an instance.
     *
     * @param methodName The method name to use for injection
     * @param injectedTypeName The parameter type of the method
     * @param primitive Is the argument type primitive
     */
    public MethodResourceInjection(final String methodName, final String injectedTypeName, final boolean primitive) {
        super(primitive);
        this.methodName = methodName;
        this.injectedTypeName = injectedTypeName;
    }

    /** {@inheritDoc} */
    protected Injector<T> getInjector(final Object target) {
        final Class<?> targetClass = target.getClass();
        final Method method;
        try {
            final Class<?> argumentType = targetClass.getClassLoader().loadClass(injectedTypeName);
            method = targetClass.getMethod(methodName, argumentType);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Target object not valid for this resource injections", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Target object not valid for this resource injections", e);
        }
        return new SetMethodInjector<T>(Values.immediateValue(target), Values.immediateValue(method));
    }
}
