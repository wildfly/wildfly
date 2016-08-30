/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.security.plugins;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.security.plugins.ClassLoaderLocator;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An implementation of {@code ClassLoaderLocator} that is based on JBoss Modules
 * @author anil saldhana
 */
public class ModuleClassLoaderLocator implements ClassLoaderLocator {
    private final ModuleLoader moduleLoader;

    public ModuleClassLoaderLocator(ModuleLoader loader) {
        this.moduleLoader = loader;
    }

    @Override
    public ClassLoader get(Set<String> configuredModules) {
        try {
            List<ClassLoader> classLoaders = new ArrayList<>();
            for (String module : configuredModules) {
                if (module != null && !module.isEmpty()) {
                    classLoaders.add(SecurityActions.getModuleClassLoader(moduleLoader, module));
                }
            }
            classLoaders.add(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
            /**
             * A Login Module can be in a custom user module.
             * The local resources (such as users.properties) can be present in a web deployment,
             * whose CL is available on the TCCL.
             */
            return new CombinedClassLoader(classLoaders.toArray(new ClassLoader[classLoaders.size()]));
        } catch (ModuleLoadException e) {
            throw SecurityLogger.ROOT_LOGGER.runtimeException(e);
        }
    }
    /** A Classloader that takes an array of Classloaders to delegate to */
    public class CombinedClassLoader extends SecureClassLoader{
        private ClassLoader[] loaders;

        public CombinedClassLoader(ClassLoader... loaders){
            this.loaders = loaders;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            for (ClassLoader loader : loaders) {
                try {
                    return loader.loadClass(name);
                } catch(ClassNotFoundException ce){
                    // do nothing, see if another loader can do this.
                }
            }
            throw new ClassNotFoundException(name);
        }

        @Override
        public URL getResource(String name) {
            URL resource = null;
            for (ClassLoader loader : loaders) {
                resource = loader.getResource(name);
                if(resource != null){
                    break;
                }
            }
            return resource;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            InputStream is = null;
            for (ClassLoader loader : loaders) {
                is = loader.getResourceAsStream(name);
                if (is != null) {
                    break;
                }
            }
            return is;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            List<URL> combinedList = new ArrayList<URL>();
            for (ClassLoader loader : loaders) {
                combinedList.addAll(Collections.list(loader.getResources(name)));
            }
            return Collections.enumeration(combinedList);
        }
    }
}
