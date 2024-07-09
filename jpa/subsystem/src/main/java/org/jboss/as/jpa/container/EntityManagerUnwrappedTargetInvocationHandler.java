/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.persistence.EntityManager;
import org.jboss.as.jpa.messages.JpaLogger;

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
            throw JpaLogger.ROOT_LOGGER.illegalCallOnCloseMethod();
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
