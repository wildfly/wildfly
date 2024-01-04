/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import java.util.List;

import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.interceptor.InvocationContext;

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
