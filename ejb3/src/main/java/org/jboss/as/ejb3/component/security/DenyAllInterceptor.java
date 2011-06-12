/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.security;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import javax.ejb.EJBAccessException;
import java.lang.reflect.Method;

/**
 * An {@link Interceptor} which can be used to prevent access to methods on a EJB.
 * <p/>
 * User: Jaikiran Pai
 */
public class DenyAllInterceptor implements Interceptor {

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);
        if (component instanceof EJBComponent == false) {
            throw new IllegalStateException("Unexpected component type: " + component.getClass() + " expected: " + EJBComponent.class);
        }
        final Method method = context.getMethod();
        final EJBComponent ejbComponent = (EJBComponent) component;
        throw new EJBAccessException("Invocation on method: " + method + " of bean: " + ejbComponent.getComponentName() + " is not allowed");
    }
}
