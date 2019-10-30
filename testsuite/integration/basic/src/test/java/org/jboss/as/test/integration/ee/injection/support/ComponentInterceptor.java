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
package org.jboss.as.test.integration.ee.injection.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Priority(Interceptor.Priority.APPLICATION + 10)
@Interceptor
@ComponentInterceptorBinding
public class ComponentInterceptor {

    private static final List<Interception> interceptions = Collections.synchronizedList(new ArrayList<Interception>());

    @AroundInvoke
    public Object alwaysReturnThis(InvocationContext ctx) throws Exception {
        interceptions.add(new Interception(ctx.getMethod().getName(), ctx.getTarget().getClass().getName()));
        return ctx.proceed();
    }

    public static void resetInterceptions() {
        interceptions.clear();
    }

    public static List<Interception> getInterceptions() {
        return interceptions;
    }

    public static class Interception {

        private final String methodName;

        private final String targetClassName;

        public Interception(String methodName, String targetClassName) {
            super();
            this.methodName = methodName;
            this.targetClassName = targetClassName;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getTargetClassName() {
            return targetClassName;
        }

    }

}
