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

package org.jboss.as.jpa.interceptor;

import org.jboss.as.jpa.container.SFSBCallStack;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Runs early in the SFSB chain, to make sure that SFSB creation operations can inherit extended persistence contexts properly
 * <p/>
 *
 * @author Stuart Douglas
 */
public class SFSBPreCreateInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SFSBPreCreateInterceptor());

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        try {
            // beginSfsbCreation() will setup a "creation time" thread local store for tracking references to extended
            // persistence contexts.
            SFSBCallStack.beginSfsbCreation();
            return interceptorContext.proceed();
        } finally {
            // bean PostCreate event lifecycle has already completed.
            // endSfsbCreation() will clear the thread local knowledge of "creation time" referenced extended
            // persistence contexts.
            SFSBCallStack.endSfsbCreation();
        }
    }

    private SFSBPreCreateInterceptor() {
    }
}
