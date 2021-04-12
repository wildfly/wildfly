/*
 * JBoss, Home of Professional Open Source
 * Copyright 2021, Red Hat Inc., and individual contributors as indicated
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

import com.sun.faces.spi.DiscoverableInjectionProvider;
import com.sun.faces.spi.InjectionProviderException;
import org.jboss.as.web.common.StartupContext;
import org.jboss.as.web.common.WebInjectionContainer;

/**
 * @author Stuart Douglas
 */
public class JSFInjectionProvider extends DiscoverableInjectionProvider {

    public static final String JAVAX_FACES = "javax.faces.";
    public static final String COM_SUN_FACES = "com.sun.faces.";
    public static final String COM_SUN_FACES_TEST = "com.sun.faces.test.";
    private final WebInjectionContainer instanceManager;

    public JSFInjectionProvider() {
        this.instanceManager = StartupContext.getInjectionContainer();
    }

    @Override
    public void inject(final Object managedBean) throws InjectionProviderException {

    }

    @Override
    public void invokePreDestroy(final Object managedBean) throws InjectionProviderException {
        if(instanceManager != null) {
            // WFLY-3820
            instanceManager.destroyInstance(managedBean);
        }
    }

    @Override
    public void invokePostConstruct(final Object managedBean) throws InjectionProviderException {
        if(managedBean.getClass().getName().startsWith(JAVAX_FACES) ||
                (managedBean.getClass().getName().startsWith(COM_SUN_FACES) && !managedBean.getClass().getName().startsWith(COM_SUN_FACES_TEST))) {
            //some internal Jakarta Server Faces instances are not destroyed properly, and they do not need to have
            //lifecycle callbacks anyway, so we don't use the instance manager to create them
            // avoid excluding elements from the Jakarta Server Faces test suite (tests fail because objects are not injected)
            return;
        }
        if(instanceManager == null) {
            // WFLY-3820
            return;
        }
        try {
            instanceManager.newInstance(managedBean);
        } catch (Exception e) {
            throw new InjectionProviderException(e);
        }
    }
}
