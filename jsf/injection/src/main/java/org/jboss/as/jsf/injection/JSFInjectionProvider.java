/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    public static final String JAVAX_FACES = "jakarta.faces.";
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
