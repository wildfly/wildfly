/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.util;

import java.lang.ref.WeakReference;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;

/**
 * AS7 version of {@link org.jboss.wsf.spi.classloading.ClassLoaderProvider}, relying on modular classloading.
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ModuleClassLoaderProvider extends ClassLoaderProvider {

    private static final ModuleIdentifier ASIL = ModuleIdentifier.create("org.jboss.as.webservices.server.integration");
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
