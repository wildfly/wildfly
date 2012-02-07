/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
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

package org.jboss.as.test.integration.ejb.interceptor.inheritorder;

import javax.ejb.Stateless;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

/**
 * @author Ondrej Chaloupka
 */
@Stateless
@Interceptors(CChildInterceptor.class)
public class CClass extends BClass {

    @Override
    @Interceptors({ CMethodInterceptor.class })
    public String run() {
        return super.run() + CClass.class.getSimpleName();
    }

    @AroundInvoke
    // does not override aroundinvoke method of BClass - both will be run
    public Object aroundInvokeCClass(final InvocationContext context) throws Exception {
        return CClass.class.getSimpleName() + ".method " + context.proceed().toString();
    }
}
