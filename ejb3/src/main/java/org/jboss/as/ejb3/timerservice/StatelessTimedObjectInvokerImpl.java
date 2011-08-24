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

import org.jboss.as.ejb3.component.stateless.StatelessSessionComponent;
import org.jboss.as.ejb3.component.stateless.StatelessSessionComponentInstance;
import org.jboss.as.ejb3.timerservice.spi.MultiTimeoutMethodTimedObjectInvoker;

import javax.ejb.Timer;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Timed object invoker for an EJB
 *
 * @author Stuart Douglas
 */
public class StatelessTimedObjectInvokerImpl implements MultiTimeoutMethodTimedObjectInvoker, Serializable {

    private final StatelessSessionComponent ejbComponent;
    private final ClassLoader classLoader;

    public StatelessTimedObjectInvokerImpl(final StatelessSessionComponent ejbComponent, final ClassLoader classLoader) {
        this.ejbComponent = ejbComponent;
        this.classLoader = classLoader;
    }

    @Override
    public void callTimeout(final Timer timer, final Method timeoutMethod) throws Exception {
        final StatelessSessionComponentInstance instance = ejbComponent.getPool().get();
        try {
            instance.invokeTimeoutMethod(timeoutMethod, timer);
        } finally {
            ejbComponent.getPool().release(instance);
        }
    }

    @Override
    public String getTimedObjectId() {
        return ejbComponent.getComponentName();
    }

    @Override
    public void callTimeout(final Timer timer) throws Exception {
        final StatelessSessionComponentInstance instance = ejbComponent.getPool().get();
        try {
            instance.invokeTimeoutMethod(timer);
        } finally {
            ejbComponent.getPool().release(instance);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
