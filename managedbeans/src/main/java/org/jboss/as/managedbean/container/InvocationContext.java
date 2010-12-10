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

package org.jboss.as.managedbean.container;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link javax.interceptor.InvocationContext} which supports the execution of managed bean method invocations
 * with an associated interceptor chain.  Uses a simple iterator to manage stepping through the interceptor chain.
 *
 * @param <T> The target object type
 *
 * @author John E. Bailey
 */
public class InvocationContext<T> implements javax.interceptor.InvocationContext {
    private T target;
    private Method method;
    private Object[] parameters;
    private final Iterator<ManagedBeanInterceptor.AroundInvokeInterceptor<?>> interceptors;
    private final Map<String, Object> contextData = Collections.emptyMap();

    /**
     * Create an instance with a set of interceptors to run around the method.
     *
     * @param target The target object of the invocation
     * @param method The method invoked
     * @param parameters The parameters to the method
     * @param interceptors The interceptor chain
     */
    public InvocationContext(final T target, final Method method, final Object[] parameters, final List<ManagedBeanInterceptor.AroundInvokeInterceptor<?>> interceptors) {
        this.target = target;
        this.method = method;
        this.parameters = parameters;
        this.interceptors = interceptors.iterator();
    }

    /** {@inheritDoc} */
    public Object getTarget() {
        return target;
    }

    /** {@inheritDoc} */
    public Method getMethod() {
        return method;
    }

    /** {@inheritDoc} */
    public Object[] getParameters() {
        return this.parameters;
    }

    /** {@inheritDoc} */
    public void setParameters(final Object[] parameters) {
        this.parameters = parameters;
    }

    /** {@inheritDoc} */
    public Map<String, Object> getContextData() {
        return contextData;
    }

    /** {@inheritDoc} */
    public Object getTimer() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation will first check to see if there are any more interceptors and if so, will use the next in the chain,
     * if not it will run the method against the target object.
     *
     * @return The result of the interceptor chain invocation.
     * @throws Exception
     */
    public Object proceed() throws Exception {
        if(interceptors.hasNext()) {
            final ManagedBeanInterceptor.AroundInvokeInterceptor<?> interceptor = interceptors.next();
            return interceptor.intercept(this);
        } else {
            return method.invoke(target, parameters);
        }
    }
}
