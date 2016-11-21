/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.spi;

import java.util.List;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.weld.spi.ComponentIntegrator.DefaultInterceptorIntegrationAction;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.manager.api.WeldManager;

/**
 * NOTE: There must be exactly one implementation available if {@link DefaultInterceptorIntegrationAction} is performed during component integration.
 * <p>
 * This implementation must be able to handle all integrated component types.
 *
 * @author Martin Kouba
 */
public interface ComponentInterceptorSupport {

    /**
     *
     * @param componentInstance
     * @return the interceptor instance for the given component
     */
    InterceptorInstances getInterceptorInstances(ComponentInstance componentInstance);

    /**
     * Set the interceptor instances to the given component.
     *
     * @param componentInstance
     * @param interceptorInstances
     */
    void setInterceptorInstances(ComponentInstance componentInstance, InterceptorInstances interceptorInstances);

    /**
     * Delegate the invocation processing.
     *
     * @return the result of subsequent interceptor method processing
     */
    Object delegateInterception(InvocationContext invocationContext, InterceptionType interceptionType, List<Interceptor<?>> currentInterceptors,
            List<Object> currentInterceptorInstances) throws Exception;

    /**
     *
     * @param componentName
     * @return the interceptor bindings
     */
    InterceptorBindings getInterceptorBindings(String componentName, WeldManager manager);

}
