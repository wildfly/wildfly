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
package org.jboss.as.ejb3.timerservice;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import javax.ejb.ConcurrentAccessException;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.Timer;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.timerservice.spi.MultiTimeoutMethodTimedObjectInvoker;

/**
 * Timed object invoker for an EJB
 *
 * @author Stuart Douglas
 */
public class PooledTimedObjectInvokerImpl implements MultiTimeoutMethodTimedObjectInvoker, Serializable {

    private final EJBComponent ejbComponent;
    private final Pool<EjbComponentInstance> pool;

    /**
     * String that uniquely identifies a deployment
     */
    private final String deploymentString;

    public PooledTimedObjectInvokerImpl(final EJBComponent ejbComponent, final String deploymentString) {
        this.ejbComponent = ejbComponent;
        this.deploymentString = deploymentString;
        this.pool = ((PooledComponent<EjbComponentInstance>) ejbComponent).getPool();
    }

    @Override
    public void callTimeout(final Timer timer, final Method timeoutMethod) throws Exception {
        final EjbComponentInstance instance = acquireInstance();
        boolean discarded = false;
        try {
            instance.invokeTimeoutMethod(timeoutMethod, timer);
        } catch (Exception ex) {
            // Detect app exception
            if (ejbComponent.getApplicationException(ex.getClass(), timeoutMethod) != null) {
                // it's an application exception, just throw it back.
                throw ex;
            }
            if (ex instanceof ConcurrentAccessTimeoutException || ex instanceof ConcurrentAccessException) {
                throw ex;
            }
            if (ex instanceof RuntimeException || ex instanceof RemoteException) {
                discarded = true;
                if (pool != null) {
                    pool.discard(instance);
                }
            }
            throw ex;
        } catch (final Error e) {
            discarded = true;
            if (pool != null) {
                pool.discard(instance);
            }
            throw e;
        } catch (final Throwable t) {
            discarded = true;
            if (pool != null) {
                pool.discard(instance);
            }
            throw new RuntimeException(t);
        } finally {
            if (!discarded) {
                releaseInstance(instance);
            }
        }
    }

    private EjbComponentInstance acquireInstance() {
        final EjbComponentInstance instance;
        if (pool != null) {
            instance = pool.get();
        } else {
            instance = (EjbComponentInstance) ejbComponent.createInstance();
        }
        return instance;
    }

    @Override
    public String getTimedObjectId() {
        return deploymentString + "." + ejbComponent.getComponentName();
    }

    @Override
    public void callTimeout(final Timer timer) throws Exception {
        final EjbComponentInstance instance = acquireInstance();
        try {
            instance.invokeTimeoutMethod(timer);
        } finally {
            releaseInstance(instance);
        }
    }

    private void releaseInstance(final EjbComponentInstance instance) {
        if (pool != null) {
            pool.release(instance);
        } else {
            instance.destroy();
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return ejbComponent.getComponentClass().getClassLoader();
    }
}
