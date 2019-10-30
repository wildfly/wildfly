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

package org.jboss.as.test.integration.ejb.remote.client.api;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * User: jpai
 */
public class InterceptorTwo {

    public static final String MESSAGE_SEPARATOR = ",";

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        final Object[] methodParams = context.getParameters();
        if (methodParams.length == 1 && methodParams[0] instanceof String) {
            final String message = (String) methodParams[0];
            final String interceptedMessage = message + MESSAGE_SEPARATOR + this.getClass().getSimpleName();
            context.setParameters(new Object[] {interceptedMessage});
        }
        return context.proceed();
    }
}
