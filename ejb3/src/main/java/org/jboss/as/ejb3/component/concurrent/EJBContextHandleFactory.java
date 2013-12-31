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
package org.jboss.as.ejb3.component.concurrent;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.concurrent.handle.ContextHandle;
import org.jboss.as.ee.concurrent.handle.ContextHandleFactory;
import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.invocation.InterceptorContext;

import javax.enterprise.concurrent.ContextService;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * The context handle factory responsible for saving and setting the ejb context. It is also responsible for starting
 * and finishing a transaction if the EJB is CMT and there was a transaction when the context is saved.
 *
 * @author Eduardo Martins
 */
public class EJBContextHandleFactory implements ContextHandleFactory {

    public static final String NAME = "EJB";

    public static final EJBContextHandleFactory INSTANCE = new EJBContextHandleFactory();

    private EJBContextHandleFactory() {
    }

    @Override
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new EJBContextHandle();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getChainPriority() {
        return 500;
    }

    @Override
    public void writeHandle(ContextHandle contextHandle, ObjectOutputStream out) throws IOException {
        out.writeObject(contextHandle);
    }

    @Override
    public ContextHandle readHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return (ContextHandle) in.readObject();
    }

    private static class EJBContextHandle implements ContextHandle {

        private final transient InterceptorContext interceptorContext;
        private transient TransactionManager transactionManager;
        private transient Transaction transaction;

        private EJBContextHandle() {
            interceptorContext = CurrentInvocationContext.get();
            if(interceptorContext != null) {
                final EJBComponent component = (EJBComponent) interceptorContext.getPrivateData(Component.class);
                if (component != null && !component.isBeanManagedTransaction()) {
                    final TransactionManager transactionManager = component.getTransactionManager();
                    try {
                        if (transactionManager != null && transactionManager.getTransaction() != null) {
                            this.transactionManager = transactionManager;
                        }
                    } catch (SystemException e) {
                        EjbLogger.ROOT_LOGGER.debug("EE Concurrency's EJB context handle failed to obtain transaction", e);
                    }
                }
            }
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        @Override
        public void setup() throws IllegalStateException {
            if(interceptorContext != null) {
                CurrentInvocationContext.push(interceptorContext);
                if (transactionManager != null) {
                    try {
                        transactionManager.begin();
                        transaction = transactionManager.getTransaction();
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }

        @Override
        public void reset() {
            if (transaction != null) {
                try {
                    transaction.commit();
                } catch (Throwable e) {
                    EjbLogger.ROOT_LOGGER.debug("EE Concurrency's EJB context handle failed to commit transaction",e);
                    try {
                        transaction.rollback();
                    } catch (Throwable e1) {
                        EjbLogger.ROOT_LOGGER.debug("EE Concurrency's EJB context handle failed to rollback transaction", e);
                    }
                } finally {
                    transaction = null;
                }
            }
            if(interceptorContext != null) {
                CurrentInvocationContext.pop();
            }
        }
    }
}
