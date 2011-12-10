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

import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.timerservice.spi.BeanRemovedException;
import org.jboss.as.ejb3.timerservice.spi.MultiTimeoutMethodTimedObjectInvoker;

/**
 * Timed object invoker for an EJB
 *
 * @author Stuart Douglas
 */
public class EntityTimedObjectInvokerImpl implements MultiTimeoutMethodTimedObjectInvoker, Serializable {

    private final EntityBeanComponent ejbComponent;

    /**
     * String that uniquely identifies a deployment
     */
    private final String deploymentString;

    public EntityTimedObjectInvokerImpl(final EntityBeanComponent ejbComponent, final String deploymentString) {
        this.ejbComponent = ejbComponent;
        this.deploymentString = deploymentString;
    }

    @Override
    public void callTimeout(final TimerImpl timer, final Method timeoutMethod) throws Exception {
        final EntityBeanComponentInstance instance;
        try {
            instance = ejbComponent.getCache().get(timer.getPrimaryKey());
            ejbComponent.getCache().reference(instance);
        } catch (Exception e) {
            //if we fail to get the EJB we assume that it is becuse the EJB no longer exists
            throw new BeanRemovedException(e);
        }
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
                instance.discard();
            }
            throw ex;
        } catch (final Error e) {
            discarded = true;
            instance.discard();
            throw e;
        } catch (final Throwable t) {
            discarded = true;
            instance.discard();
            throw new RuntimeException(t);
        } finally {
            if (!discarded) {
                ejbComponent.getCache().release(instance, true);
            }
        }
    }

    @Override
    public String getTimedObjectId() {
        return deploymentString + "." + ejbComponent.getComponentName();
    }

    @Override
    public void callTimeout(final TimerImpl timer) throws Exception {
        callTimeout(timer, ejbComponent.getTimeoutMethod());
    }


    @Override
    public ClassLoader getClassLoader() {
        return ejbComponent.getComponentClass().getClassLoader();
    }
}
