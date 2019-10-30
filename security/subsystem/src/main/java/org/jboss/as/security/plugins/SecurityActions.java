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

package org.jboss.as.security.plugins;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.List;

import org.wildfly.security.manager.action.GetModuleClassLoaderAction;
import org.jboss.as.security.plugins.ModuleClassLoaderLocator.CombinedClassLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.security.manager.WildFlySecurityManager;

import static java.security.AccessController.doPrivileged;

/**
 * Privileged blocks for this package
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
class SecurityActions {

    static ModuleClassLoader getModuleClassLoader(final ModuleLoader loader, final String moduleSpec) throws ModuleLoadException {
        final Module module = loader.loadModule(ModuleIdentifier.fromString(moduleSpec));
        return WildFlySecurityManager.isChecking() ? doPrivileged(new GetModuleClassLoaderAction(module)) : module.getClassLoader();
    }

    static SecurityContext getSecurityContext() {
        if (WildFlySecurityManager.isChecking()) {
            return doPrivileged(new PrivilegedAction<SecurityContext>() {
                public SecurityContext run() {
                    return SecurityContextAssociation.getSecurityContext();
                }
            });
        } else {
            return SecurityContextAssociation.getSecurityContext();
        }
    }

    static Principal getPrincipal() {
        if (WildFlySecurityManager.isChecking()) {
            return doPrivileged(new PrivilegedAction<Principal>() {
                public Principal run() {
                    Principal principal = null;
                    SecurityContext sc = getSecurityContext();
                    if (sc != null) {
                        principal = sc.getUtil().getUserPrincipal();
                    }
                    return principal;
                }
            });
        } else {
            Principal principal = null;
            SecurityContext sc = getSecurityContext();
            if (sc != null) {
                principal = sc.getUtil().getUserPrincipal();
            }
            return principal;
        }
    }

    static Object getCredential() {
        if (WildFlySecurityManager.isChecking()) {
            return doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    Object credential = null;
                    SecurityContext sc = getSecurityContext();
                    if (sc != null) {
                        credential = sc.getUtil().getCredential();
                    }
                    return credential;
                }
            });
        } else {
            Object credential = null;
            SecurityContext sc = getSecurityContext();
            if (sc != null) {
                credential = sc.getUtil().getCredential();
            }
            return credential;
        }
    }

    /**
     * Returns the <code>ModuleClassLoaderLocator.CombinedClassLoader</code> instance with consideration of security manager enabled
     *
     * @param classLoaders the delegated ClassLoaders
     * @return the <code>ModuleClassLoaderLocator.CombinedClassLoader</code> instance
     */
    static ModuleClassLoaderLocator.CombinedClassLoader createCombinedClassLoader(final List<ClassLoader> classLoaders) {
        if (WildFlySecurityManager.isChecking()) {
            return doPrivileged(new PrivilegedAction<ModuleClassLoaderLocator.CombinedClassLoader>() {
                @Override
                public CombinedClassLoader run() {
                    return new ModuleClassLoaderLocator.CombinedClassLoader(classLoaders);
                }
            });
        } else {
            return new ModuleClassLoaderLocator.CombinedClassLoader(classLoaders);
        }
    }

}