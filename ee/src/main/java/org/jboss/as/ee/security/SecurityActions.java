/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.security;

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;

import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
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
        final Module module = loader.loadModule(ModuleIdentifierUtil.parseCanonicalModuleIdentifier(moduleSpec));
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
