/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.interceptors;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class AbstractEJBInterceptor implements Interceptor {

    protected static <C extends EJBComponent> C getComponent(InterceptorContext context, Class<C> componentType) {
        Component component = context.getPrivateData(Component.class);
        if (component == null) {
           throw EjbLogger.ROOT_LOGGER.componentNotSetInInterceptor(context);
        }
        return componentType.cast(component);
    }
}
