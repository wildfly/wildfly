/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.interceptors;

/**
 * Interceptor factory that handles user interceptors, and switches between timer normal invocations
 *
 * @author Stuart Douglas
 */

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;


public class UserInterceptorFactory implements InterceptorFactory {
    private final InterceptorFactory aroundInvoke;
    private final InterceptorFactory aroundTimeout;

    public UserInterceptorFactory(final InterceptorFactory aroundInvoke, final InterceptorFactory aroundTimeout) {
        this.aroundInvoke = aroundInvoke;
        this.aroundTimeout = aroundTimeout;
    }


    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        final Interceptor aroundInvoke = this.aroundInvoke.create(context);
        final Interceptor aroundTimeout;
        if(this.aroundTimeout != null) {
            aroundTimeout = this.aroundTimeout.create(context);
        } else {
            aroundTimeout = null;
        }
        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {
                final InvocationType marker = context.getPrivateData(InvocationType.class);
                if (marker == InvocationType.TIMER) {
                    return aroundTimeout.processInvocation(context);
                } else {
                    return aroundInvoke.processInvocation(context);
                }
            }
        };

    }
}
