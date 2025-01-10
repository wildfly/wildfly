/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.util;

import java.lang.ref.WeakReference;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;

/**
 * AS7 version of {@link org.jboss.wsf.spi.classloading.ClassLoaderProvider}, relying on modular classloading.
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ModuleClassLoaderProvider extends ClassLoaderProvider {

    private static final String ASIL = "org.jboss.as.webservices.server.integration";
    private WeakReference<ClassLoader> integrationClassLoader;

    @Override
    public ClassLoader getWebServiceSubsystemClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public ClassLoader getServerIntegrationClassLoader() {
        if (integrationClassLoader == null || integrationClassLoader.get() == null) {
            try {
                Module module = Module.getBootModuleLoader().loadModule(ASIL);
                integrationClassLoader = new WeakReference<ClassLoader>(module.getClassLoader());
            } catch (ModuleLoadException e) {
                throw new RuntimeException(e);
            }
        }
        return integrationClassLoader.get();
    }

    @Override
    public ClassLoader getServerJAXRPCIntegrationClassLoader() {
        throw new UnsupportedOperationException();
    }

    public static void register() {
        ClassLoaderProvider.setDefaultProvider(new ModuleClassLoaderProvider());
    }

}
