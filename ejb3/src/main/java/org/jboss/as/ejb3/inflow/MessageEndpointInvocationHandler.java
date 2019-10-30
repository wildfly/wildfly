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
package org.jboss.as.ejb3.inflow;

import javax.resource.ResourceException;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.LocalTransactionException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageEndpointInvocationHandler extends AbstractInvocationHandler implements MessageEndpoint {
    private final MessageEndpointService service;
    private final Object delegate;
    private final XAResource xaRes;
    private final AtomicBoolean released = new AtomicBoolean(false);

    private Transaction currentTx;
    private ClassLoader previousClassLoader;
    private Transaction previousTx;

    MessageEndpointInvocationHandler(final MessageEndpointService service, final Object delegate, final XAResource xaResource) {
        this.service = service;
        this.delegate = delegate;
        this.xaRes = xaResource;
    }

    @Override
    public void afterDelivery() throws ResourceException {
        final TransactionManager tm = getTransactionManager();
        try {
            if (currentTx != null) {
                if (currentTx.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                    tm.rollback();
                else
                    tm.commit();
                currentTx = null;
            }
            if (previousTx != null) {
                tm.resume(previousTx);
                previousTx = null;
            }
        } catch (InvalidTransactionException e) {
            throw new LocalTransactionException(e);
        } catch (HeuristicMixedException e) {
            throw new LocalTransactionException(e);
        } catch (SystemException e) {
            throw new LocalTransactionException(e);
        } catch (HeuristicRollbackException e) {
            throw new LocalTransactionException(e);
        } catch (RollbackException e) {
            throw new LocalTransactionException(e);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(previousClassLoader);
            previousClassLoader = null;
        }
    }

    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        // JCA 1.6 FR 13.5.6
        // The application server must set the thread context class loader to the endpoint
        // application class loader during the beforeDelivery call.
        previousClassLoader = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(getApplicationClassLoader());
        try {
            final TransactionManager tm = getTransactionManager();
            // TODO: in violation of JCA 1.6 FR 13.5.9?
            previousTx = tm.suspend();
            boolean isTransacted = service.isDeliveryTransacted(method);
            if (isTransacted) {
                tm.begin();
                currentTx = tm.getTransaction();
                if (xaRes != null)
                    currentTx.enlistResource(xaRes);
            }
        } catch (Throwable t) {
            throw new ApplicationServerInternalException(t);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(previousClassLoader);
        }
    }

    @Override
    protected boolean doEquals(Object obj) {
        if (!(obj instanceof MessageEndpointInvocationHandler))
            return false;

        return delegate.equals(((MessageEndpointInvocationHandler) obj).delegate);
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Are we still usable?
        if (released.get())
            throw EjbLogger.ROOT_LOGGER.messageEndpointAlreadyReleased(this);

        // TODO: check for concurrent invocation

        if (method.getDeclaringClass().equals(MessageEndpoint.class))
            return handle(method, args);

        // TODO: Option A
        try {
            return method.invoke(delegate, args);
        }
        catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    protected final ClassLoader getApplicationClassLoader() {
        return this.service.getClassLoader();
    }

    protected final TransactionManager getTransactionManager() {
        return service.getTransactionManager();
    }

    @Override
    public void release() {
        if (released.getAndSet(true))
            throw new IllegalStateException("Message endpoint " + this + " has already been released");

        // TODO: tidy up outstanding delivery

        service.release(delegate);
    }
}
