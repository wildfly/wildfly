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

package org.jboss.as.ee.component;

import org.jboss.invocation.CannotProceedException;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * The dispatcher interceptor.  Last interceptor in the component-level interceptor chain which
 * dispatches the invocation to the per-instance chain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class DispatcherInterceptor implements Interceptor {

    public static final Interceptor INSTANCE = new DispatcherInterceptor();

    public Object processInvocation(final InterceptorContext context) throws Exception {
        // Get the appropriate method from the previously associated instance
        final ComponentInstance instance = context.getPrivateData(ComponentInstance.class);
        if (instance == null) {
            throw new CannotProceedException("No instance is associated with this component class");
        }
        context.setTarget(instance.getInstance());
        final Interceptor interceptor = instance.getInterceptor(context.getMethod());
        return interceptor.processInvocation(context);
    }
}
