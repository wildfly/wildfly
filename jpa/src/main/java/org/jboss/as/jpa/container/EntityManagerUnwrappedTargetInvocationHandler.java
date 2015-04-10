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

package org.jboss.as.jpa.container;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;

/**
 * Handle method execution delegation to a wrapped object using the passed entity manager to obtain the target
 * invocation target.  Forked from Emmanuel's org.jboss.ejb3.entity.hibernate.TransactionScopedSessionInvocationHandler.
 *
 * @author Emmanuel Bernard
 * @author Scott Marlow
 */
public class EntityManagerUnwrappedTargetInvocationHandler implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 5254527687L;

    private Class<?> wrappedClass;
    private EntityManager targetEntityManager;

    public EntityManagerUnwrappedTargetInvocationHandler() {

    }

    public EntityManagerUnwrappedTargetInvocationHandler(EntityManager targetEntityManager, Class<?> wrappedClass) {
        this.targetEntityManager = targetEntityManager;
        this.wrappedClass = wrappedClass;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("close".equals(method.getName())) {
            throw new IllegalStateException("Illegal to call this method from injected, managed EntityManager");
        } else {
            //catch all
            try {
                return method.invoke(getWrappedObject(), args);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException) e.getTargetException();
                } else {
                    throw e;
                }
            }
        }
    }

    private Object getWrappedObject() {
        return targetEntityManager.unwrap(wrappedClass);
    }

}
