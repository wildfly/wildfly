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
package org.jboss.as.weld.services;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.weld.WeldMessages;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.weld.bootstrap.api.Singleton;
import org.jboss.weld.bootstrap.api.SingletonProvider;

/**
 * Singleton Provider that uses the TCCL to figure out the current application.
 *
 * @author Stuart Douglas
 */
public class ModuleGroupSingletonProvider extends SingletonProvider {

    /**
     * Map of the top level class loader to all class loaders in a deployment
     */
    public static Map<ClassLoader, Set<ClassLoader>> deploymentClassLoaders = new ConcurrentHashMap<ClassLoader, Set<ClassLoader>>();

    /**
     * Maps a top level class loader to all CL's in the deployment
     */
    public static void addClassLoaders(ClassLoader topLevel, Set<ClassLoader> allClassLoaders) {
        deploymentClassLoaders.put(topLevel, allClassLoaders);
    }

    /**
     * Removes the class loader mapping
     */
    public static void removeClassLoader(ClassLoader topLevel) {
        deploymentClassLoaders.remove(topLevel);
    }

    @Override
    public <T> Singleton<T> create(Class<? extends T> type) {
        return new TCCLSingleton<T>();
    }

    private static class TCCLSingleton<T> implements Singleton<T> {

        private volatile Map<ClassLoader, T> store = Collections.emptyMap();

        public T get() {
            T instance = store.get(findParentModuleCl(getClassLoader()));
            if (instance == null) {
                throw WeldMessages.MESSAGES.singletonNotSet(getClassLoader());
            }
            return instance;
        }

        public synchronized void set(T object) {
            final Map<ClassLoader, T> store = new IdentityHashMap<ClassLoader, T>(this.store);
            ClassLoader classLoader = getClassLoader();
            store.put(classLoader, object);
            if (deploymentClassLoaders.containsKey(classLoader)) {
                for (ClassLoader cl : deploymentClassLoaders.get(classLoader)) {
                    store.put(cl, object);
                }
            }
            this.store = store;
        }

        public synchronized void clear() {
            ClassLoader classLoader = getClassLoader();
            final Map<ClassLoader, T> store = new IdentityHashMap<ClassLoader, T>(this.store);
            store.remove(classLoader);
            if (deploymentClassLoaders.containsKey(classLoader)) {
                for (ClassLoader cl : deploymentClassLoaders.get(classLoader)) {
                    store.remove(cl);
                }
            }
            this.store = store;
        }

        public boolean isSet() {
            return store.containsKey(findParentModuleCl(getClassLoader()));
        }

        /**
         * If a custom CL is in use we want to get the module CL it delegates to
         * @param classLoader The current CL
         * @returnThe corresponding module CL
         */
        private ClassLoader findParentModuleCl(ClassLoader classLoader) {
            ClassLoader c = classLoader;
            while (c != null && !(c instanceof ModuleClassLoader)) {
                c = c.getParent();
            }
            return c;
        }

        private ClassLoader getClassLoader() {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
            } else {
                return Thread.currentThread().getContextClassLoader();
            }
        }
    }
}
