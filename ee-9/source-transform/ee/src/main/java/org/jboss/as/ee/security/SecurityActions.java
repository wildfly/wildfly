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

package org.jboss.as.ee.security;

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.action.GetModuleClassLoaderAction;

/**
 * Privileged blocks for this package
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Anil Saldhana
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SecurityActions {

    static ModuleClassLoader getModuleClassLoader(final String moduleSpec) throws ModuleLoadException {
        ModuleLoader loader = Module.getCallerModuleLoader();
        final Module module = loader.loadModule(ModuleIdentifier.fromString(moduleSpec));
        GetModuleClassLoaderAction action = new GetModuleClassLoaderAction(module);
        return WildFlySecurityManager.isChecking() ? doPrivileged(action) : action.run();
    }

    static ClassLoader setThreadContextClassLoader(ClassLoader toSet) {
        return classLoaderActions().setThreadContextClassLoader(toSet);
    }

    private static ClassLoaderActions classLoaderActions() {
        return WildFlySecurityManager.isChecking() ? ClassLoaderActions.PRIVILEGED : ClassLoaderActions.NON_PRIVILEGED;
    }

    private interface ClassLoaderActions {

        ClassLoader setThreadContextClassLoader(ClassLoader toSet);

        ClassLoaderActions NON_PRIVILEGED = new ClassLoaderActions() {

            @Override
            public ClassLoader setThreadContextClassLoader(ClassLoader toSet) {
                Thread currentThread = Thread.currentThread();
                ClassLoader previous = currentThread.getContextClassLoader();
                currentThread.setContextClassLoader(toSet);
                return previous;
            }
        };

        ClassLoaderActions PRIVILEGED = new ClassLoaderActions() {

            @Override
            public ClassLoader setThreadContextClassLoader(final ClassLoader toSet) {
                return doPrivileged(new PrivilegedAction<ClassLoader>() {

                    @Override
                    public ClassLoader run() {
                        return NON_PRIVILEGED.setThreadContextClassLoader(toSet);
                    }
                });
            }
        };
    }

}
