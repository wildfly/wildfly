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

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.as.security.SecurityMessages;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;

/**
 * Privileged blocks for this package
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
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

    static SecurityContext getSecurityContext() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {
                public SecurityContext run() {
                    return SecurityContextAssociation.getSecurityContext();
                }
            });
        } else {
            return SecurityContextAssociation.getSecurityContext();
        }
    }

    static Principal getPrincipal() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Principal>() {
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
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Object>() {
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

    static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }
}