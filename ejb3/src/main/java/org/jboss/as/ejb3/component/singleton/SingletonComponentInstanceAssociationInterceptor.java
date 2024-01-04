/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Responsible for associating the single component instance for a singleton bean during invocation.
 *
 * @author Jaikiran Pai
 */
public class SingletonComponentInstanceAssociationInterceptor extends AbstractEJBInterceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SingletonComponentInstanceAssociationInterceptor());

    private SingletonComponentInstanceAssociationInterceptor() {

    }

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        SingletonComponent singletonComponent = getComponent(interceptorContext, SingletonComponent.class);
        ComponentInstance singletonComponentInstance = singletonComponent.getComponentInstance();
        if (singletonComponentInstance == null) {
            throw EjbLogger.ROOT_LOGGER.componentInstanceNotAvailable(interceptorContext);
        }
        interceptorContext.putPrivateData(ComponentInstance.class, singletonComponentInstance);
        return interceptorContext.proceed();
    }
}
