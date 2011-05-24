/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.component.session;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import javax.ejb.EJBException;
import java.lang.reflect.Method;

/**
 * @author Stuart Douglas
 */
public class NotBusinessMethodInterceptor implements Interceptor {

    private final Method method;

    public NotBusinessMethodInterceptor(final Method method) {
        this.method = method;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        throw new EJBException("Not a business method " + method + ". Do not call non-public methods on EJB's");
    }
}
