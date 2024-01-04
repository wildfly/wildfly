/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.context;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.common.function.ThreadLocalStack;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class CurrentInvocationContext {
    private static final ThreadLocalStack<InterceptorContext> stack = new ThreadLocalStack<InterceptorContext>();

    public static InterceptorContext get() {
        InterceptorContext current = stack.peek();
        return current;
    }

    public static EJBContextImpl getEjbContext() {
        final InterceptorContext context = get();
        if(context == null) {
            throw EjbLogger.ROOT_LOGGER.noEjbContextAvailable();
        }
        final ComponentInstance component = context.getPrivateData(ComponentInstance.class);
        if(!(component instanceof EjbComponentInstance)) {
            throw EjbLogger.ROOT_LOGGER.currentComponentNotAEjb(component);
        }
        return ((EjbComponentInstance)component).getEjbContext();
    }

    public static InterceptorContext pop() {
        return stack.pop();
    }

    public static void push(InterceptorContext invocation) {
        assert invocation != null : "invocation is null";
        stack.push(invocation);
    }

}
