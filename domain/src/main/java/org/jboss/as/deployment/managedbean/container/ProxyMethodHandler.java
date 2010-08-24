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

package org.jboss.as.deployment.managedbean.container;

import javax.interceptor.ExcludeClassInterceptors;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Method handler used to proxy managed bean method invocations.  For each method called it will check to see if the method
 * supports interception and will execute a new {@link InvocationContext}.  If the method does not support interceptors,
 * it will run the method directly on the managed bean instance. 
 *
 * @author John E. Bailey
 */
public class ProxyMethodHandler<T> {
    private final T mangedBeanInstance;
    private final List<ManagedBeanInterceptor.AroundInvokeInterceptor<?>> interceptors;

    /**
     * Create an instance.
     *
     * @param mangedBeanInstance The managed bean instance
     * @param interceptors The interceptor chain
     */
    public ProxyMethodHandler(T mangedBeanInstance, List<ManagedBeanInterceptor.AroundInvokeInterceptor<?>> interceptors) {
        this.mangedBeanInstance = mangedBeanInstance;
        this.interceptors = interceptors;
    }

    /**
     * Invoke a method on a managed bean instance method.
     *
     * @param self {@code this}
     * @param thisMethod The method on the proxy
     * @param proceed The original method on the target
     * @param arguments The arguments to the method invocation
     * @return The value of the invocation context execution
     */
    public Object invoke(final Object self, final Method thisMethod, final Method proceed, final Object[] arguments) throws Throwable {
        if(!proceed.isAnnotationPresent(ExcludeClassInterceptors.class)) {
            return new InvocationContext(mangedBeanInstance, proceed, arguments, interceptors).proceed();
        }
        return proceed.invoke(mangedBeanInstance, arguments);
    }
}
