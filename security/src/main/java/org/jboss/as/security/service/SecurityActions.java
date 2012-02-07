/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security.service;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Security;

import org.jboss.as.security.SecurityMessages;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * Privileged blocks for this package
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Anil Saldhana
 */
class SecurityActions {

    static ModuleClassLoader getModuleClassLoader(final String moduleSpec) throws ModuleLoadException {
        if (System.getSecurityManager() != null) {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ModuleClassLoader>() {
                    public ModuleClassLoader run() throws ModuleLoadException {
                        ModuleLoader loader = Module.getCallerModuleLoader();
                        ModuleIdentifier identifier = ModuleIdentifier.fromString(moduleSpec);
                        return loader.loadModule(identifier).getClassLoader();
                    }
                });
            } catch (PrivilegedActionException pae) {
                throw SecurityMessages.MESSAGES.moduleLoadException(pae);
            }
        } else {
            ModuleLoader loader = Module.getCallerModuleLoader();
            ModuleIdentifier identifier = ModuleIdentifier.fromString(moduleSpec);
            return loader.loadModule(identifier).getClassLoader();
        }
    }

    static void setSecurityProperty(final String key, final String value) {
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    Security.setProperty(key, value);
                    return null;
                }
            });
        } else {
            Security.setProperty(key, value);
        }
    }

    static String getSystemProperty(final String name, final String defaultValue) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(name, defaultValue);
                }
            });
        } else {
            return System.getProperty(name, defaultValue);
        }
    }

    static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
        } else {
            return Thread.currentThread().getContextClassLoader();
        }
    }

    static Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (System.getSecurityManager() != null) {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    public Class<?> run() throws ClassNotFoundException {
                        ClassLoader[] cls = new ClassLoader[] { SecurityActions.class.getClassLoader(), // PB classes (not always on TCCL [modular env])
                                getContextClassLoader(), // User defined classes
                                ClassLoader.getSystemClassLoader() // System loader, usually has app class path
                        };
                        ClassNotFoundException e = null;
                        for (ClassLoader cl : cls) {
                            if (cl == null)
                                continue;

                            try {
                                return cl.loadClass(name);
                            } catch (ClassNotFoundException ce) {
                                e = ce;
                            }
                        }
                        throw e != null ? e : SecurityMessages.MESSAGES.cnfe(name);
                    }
                });
            } catch (PrivilegedActionException pae) {
                throw SecurityMessages.MESSAGES.cnfeThrow(name, pae);
            }
        } else {
            ClassLoader[] cls = new ClassLoader[] { SecurityActions.class.getClassLoader(), // PB classes (not always on TCCL [modular env])
                    getContextClassLoader(), // User defined classes
                    ClassLoader.getSystemClassLoader() // System loader, usually has app class path
            };
            ClassNotFoundException e = null;
            for (ClassLoader cl : cls) {
                if (cl == null)
                    continue;

                try {
                    return cl.loadClass(name);
                } catch (ClassNotFoundException ce) {
                    e = ce;
                }
            }
            throw e != null ? e : SecurityMessages.MESSAGES.cnfe(name);
        }
    }
}