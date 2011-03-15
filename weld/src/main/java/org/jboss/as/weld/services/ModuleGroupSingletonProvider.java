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

import org.jboss.weld.bootstrap.api.Singleton;
import org.jboss.weld.bootstrap.api.SingletonProvider;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    public static void addClassLoaders(ClassLoader topLevel,Set<ClassLoader> allClassLoaders) {
        deploymentClassLoaders.put(topLevel,allClassLoaders);
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
        // use Hashtable for concurrent access
        private final Map<ClassLoader, T> store = new Hashtable<ClassLoader, T>();

        public T get() {
            T instance = store.get(getClassLoader());
            if (instance == null) {
                throw new IllegalStateException("Singleton not set for " + getClassLoader());
            }
            return instance;
        }

        public void set(T object) {
            ClassLoader classLoader = getClassLoader();
            store.put(classLoader, object);
            if(deploymentClassLoaders.containsKey(classLoader)) {
                for(ClassLoader cl : deploymentClassLoaders.get(classLoader)) {
                    store.put(cl,object);
                }
            }
        }

        public void clear() {
            ClassLoader classLoader = getClassLoader();
            store.remove(classLoader);
            if(deploymentClassLoaders.containsKey(classLoader)) {
                for(ClassLoader cl : deploymentClassLoaders.get(classLoader)) {
                    store.remove(cl);
                }
            }
        }

        public boolean isSet() {
            return store.containsKey(getClassLoader());
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
