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

package org.jboss.as.ee.managedbean.component;

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ManagedBeanDestroyInterceptor implements Interceptor {
    private final AtomicReference<ComponentInstance> componentInstanceReference;

    public ManagedBeanDestroyInterceptor(final AtomicReference<ComponentInstance> componentInstanceReference) {
        this.componentInstanceReference = componentInstanceReference;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final ComponentInstance componentInstance = componentInstanceReference.get();
        context.putPrivateData(ComponentInstance.class, componentInstance);
        try {
            return context.proceed();
        } finally {
            context.putPrivateData(ComponentInstance.class, null);
            componentInstance.destroy();
        }
    }
}
