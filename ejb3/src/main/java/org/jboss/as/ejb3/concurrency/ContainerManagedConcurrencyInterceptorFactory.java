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

package org.jboss.as.ejb3.concurrency;

import org.jboss.as.ee.component.Component;
import org.jboss.ejb3.concurrency.spi.LockableComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * An {@link InterceptorFactory} which returns a new instance of {@link ContainerManagedConcurrencyInterceptor} on each
 * invocation to {@link #create(org.jboss.invocation.InterceptorFactoryContext)}. This {@link InterceptorFactory} can be used
 * for handling container managed concurrency invocations on a {@link LockableComponent}
 * <p/>
 * User: Jaikiran Pai
 */
public class ContainerManagedConcurrencyInterceptorFactory implements InterceptorFactory {
    @Override
    public Interceptor create(InterceptorFactoryContext context) {
        final Component component = (Component) context.getContextData().get(Component.class);
        if (component == null) {
            throw new IllegalStateException("Component not found in interceptor factory context: " + context);
        }
        if (component instanceof LockableComponent == false) {
            throw new IllegalStateException("Component " + component + " is of type: " + component.getClass().getName() + " expected type: " + LockableComponent.class.getName());
        }
        return new ContainerManagedConcurrencyInterceptor((LockableComponent) component);
    }
}
