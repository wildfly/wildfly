/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.context;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.util.ThreadLocalStack;
import org.jboss.invocation.InterceptorContext;

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
