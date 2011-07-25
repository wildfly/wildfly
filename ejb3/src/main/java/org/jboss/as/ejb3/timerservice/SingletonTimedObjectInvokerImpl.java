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

import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.ejb3.timerservice.spi.MultiTimeoutMethodTimedObjectInvoker;

import javax.ejb.Timer;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Timed object invoker for singleton EJB's
 *
 * @author Stuart Douglas
 */
public class SingletonTimedObjectInvokerImpl implements MultiTimeoutMethodTimedObjectInvoker, Serializable {

    private final SingletonComponent ejbComponent;
    private final ClassLoader classLoader;

    public SingletonTimedObjectInvokerImpl(final SingletonComponent ejbComponent, final ClassLoader classLoader) {
        this.ejbComponent = ejbComponent;
        this.classLoader = classLoader;
    }

    @Override
    public void callTimeout(final Timer timer, final Method timeoutMethod) throws Exception {
        ejbComponent.getComponentInstance().invokeTimeoutMethod(timeoutMethod, timer);
    }

    @Override
    public String getTimedObjectId() {
        return ejbComponent.getComponentName();
    }

    @Override
    public void callTimeout(final Timer timer) throws Exception {
        ejbComponent.getComponentInstance().invokeTimeoutMethod(timer);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
