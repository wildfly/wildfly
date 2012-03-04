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

package org.jboss.as.capedwarf.api;

import org.jboss.modules.ModuleClassLoader;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Security actions.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
final class SecurityActions {

    static ClassLoader getAppClassLoader() {
        final ClassLoader tccl = getTCCL();
        // unwrap -- might be WebEnvCL
        ClassLoader cl = tccl;
        while (cl != null && (cl instanceof ModuleClassLoader == false)) {
            cl = cl.getParent();
        }
        if (cl == null)
            throw new IllegalArgumentException("No ModuleClassLoader in hierarchy?! - " + tccl);
        return cl;
    }

    private static ClassLoader getTCCL() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
        }
    }

}
