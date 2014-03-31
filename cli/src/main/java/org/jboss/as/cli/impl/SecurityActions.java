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
package org.jboss.as.cli.impl;

import static java.lang.Runtime.getRuntime;
import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.action.AddShutdownHookAction;
import org.wildfly.security.manager.action.GetModuleClassLoaderAction;

/**
 * Package privileged actions
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Scott.Stark@jboss.org
 * @author Alexey Loubyansky
 */
class SecurityActions {
    static void addShutdownHook(Thread hook) {
        if (! WildFlySecurityManager.isChecking()) {
            getRuntime().addShutdownHook(hook);
        } else {
            doPrivileged(new AddShutdownHookAction(hook));
        }
    }

    static <T> T loadAndInstantiateFromClassClassLoader(final Class<?> base, final Class<T> iface, final String name) throws Exception {
        if (!WildFlySecurityManager.isChecking()){
            return internalLoadAndInstantiateFromClassClassLoader(base, iface, name);
        } else {
            try {
                return doPrivileged(new PrivilegedExceptionAction<T>() {
                    @Override
                    public T run() throws Exception {
                        return internalLoadAndInstantiateFromClassClassLoader(base, iface, name);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof RuntimeException){
                    throw (RuntimeException)t;
                }
                throw new Exception(t);
            }
        }
    }

    private static <T> T internalLoadAndInstantiateFromClassClassLoader(Class<?> base, Class<T> iface, String name) throws Exception {
        ClassLoader cl = base.getClassLoader();
        if (cl == null){
            cl = ClassLoader.getSystemClassLoader();
        }
        Class<?> clazz = cl.loadClass(name);
        return iface.cast(clazz.newInstance());
    }

    static <T> T loadAndInstantiateFromModule(final String moduleId, final Class<T> iface, final String name) throws Exception {
        if (!WildFlySecurityManager.isChecking()) {
            return internalLoadAndInstantiateFromModule(moduleId, iface, name);
        } else {
            try {
                return doPrivileged(new PrivilegedExceptionAction<T>() {
                    @Override
                    public T run() throws Exception {
                        return internalLoadAndInstantiateFromModule(moduleId, iface, name);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof RuntimeException){
                    throw (RuntimeException)t;
                }
                throw new Exception(t);
            }
        }
    }

    private static <T> T internalLoadAndInstantiateFromModule(String moduleId, final Class<T> iface, final String name) throws Exception {
        ModuleLoader loader = Module.getCallerModuleLoader();
        final Module module = loader.loadModule(ModuleIdentifier.fromString(moduleId));
        ClassLoader cl = WildFlySecurityManager.isChecking() ? doPrivileged(new GetModuleClassLoaderAction(module)) : module.getClassLoader();
        Class<?> clazz = cl.loadClass(name);
        return iface.cast(clazz.newInstance());
    }

}
