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
package org.jboss.as.jsf.injection;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.faces.FacesException;
import javax.naming.NamingException;

import org.apache.myfaces.config.annotation.LifecycleProvider2;
import org.jboss.as.web.common.StartupContext;
import org.jboss.as.web.common.WebInjectionContainer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class MyFacesLifecycleProvider implements LifecycleProvider2 {

    private final WebInjectionContainer injectionContainer;

    public MyFacesLifecycleProvider() {
        this.injectionContainer = StartupContext.getInjectionContainer();
    }

    public Object newInstance(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NamingException, InvocationTargetException {
        final Class<?> clazz = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged().loadClass(className);
        if (System.getSecurityManager() == null) {
            return clazz.newInstance();
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public Object run() throws IllegalAccessException, InstantiationException {
                        return clazz.newInstance();
                    }
                });
            } catch (PrivilegedActionException pae) {
                Exception e = pae.getException();
                if (e instanceof IllegalAccessException) {
                    throw (IllegalAccessException)e;
                } else if (e instanceof InstantiationException) {
                    throw (InstantiationException)e;
                } else {
                    throw new FacesException(e);
                }
            }
        }
    }

    public void postConstruct(Object obj) throws IllegalAccessException, InvocationTargetException {
        // WFLY-3387 MyFaces injects managed properties before calling this method
        try {
            injectionContainer.newInstance(obj);
        } catch (NamingException e) {
            throw new FacesException(e);
        }
    }

    public void destroyInstance(Object obj) throws IllegalAccessException, InvocationTargetException {
        injectionContainer.destroyInstance(obj);
    }

}
