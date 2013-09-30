/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.narayana.txframework.impl.ServiceInvocationMeta;
import org.jboss.narayana.txframework.impl.handlers.HandlerFactory;
import org.jboss.narayana.txframework.impl.handlers.ProtocolHandler;

import java.lang.reflect.Method;

/**
 * @author paul.robinson@redhat.com 01/02/2013
 */
public class XTSPOJOInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new XTSPOJOInterceptor());

    protected XTSPOJOInterceptor() {
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {

        ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
        Object serviceInstance = componentInstance.getInstance();
        Method serviceMethod = context.getMethod();
        Class serviceClass = serviceInstance.getClass();

        Object result;
        ProtocolHandler protocolHandler = HandlerFactory.getInstance(new ServiceInvocationMeta(serviceInstance, serviceClass, serviceMethod));
        try {
            protocolHandler.preInvocation();
            result = context.proceed();
            protocolHandler.notifySuccess();
        } catch (Exception e) {
            protocolHandler.notifyFailure();
            throw e;
        }

        return result;
    }
}
