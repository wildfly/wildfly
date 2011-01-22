/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.jpa.mbean;

import javax.naming.InitialContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * For the moment there is no remoting, so we need to call the JPA from within.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class Test implements TestMBean {
    @Override
    public Object exec(Class<?> cls) throws Exception {
        Callable<?> callable = (Callable<?>) cls.newInstance();
        return callable.call();
    }

    @Override
   public Object invoke(String name, String methodName, Class<?>[] parameterTypes, Object[] params) throws Exception {
      InitialContext ctx = new InitialContext();
      Object bean = ctx.lookup(name);
      Method method = bean.getClass().getMethod(methodName, parameterTypes);
      try {
         return method.invoke(bean, params);
      }
      catch(InvocationTargetException e) {
         Throwable t = e.getTargetException();
         if (t instanceof Exception)
            throw (Exception) t;
         if (t instanceof Error)
            throw (Error) t;
         throw e;
      }
   }
}
