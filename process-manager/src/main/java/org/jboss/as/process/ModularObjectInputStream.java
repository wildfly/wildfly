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

package org.jboss.as.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ModularObjectInputStream extends ObjectInputStream {

    private ModularObjectInputStream(final InputStream in) throws IOException {
        super(in);
    }

    static ModularObjectInputStream create(final InputStream in) throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ModularObjectInputStream>() {
                    public ModularObjectInputStream run() throws IOException {
                        return new ModularObjectInputStream(in);
                    }
                });
            } catch (PrivilegedActionException e) {
                final Throwable t = e.getCause();
                if (t instanceof IOException) {
                    throw (IOException) t;
                } else if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else if (t instanceof Error) {
                    throw (Error) t;
                } else {
                    throw new RuntimeException("Unexpected action exception", t);
                }
            }
        } else {
            return new ModularObjectInputStream(in);
        }
    }

    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        ModuleIdentifier id = (ModuleIdentifier) readObject();
        if (id == null) {
            return super.resolveClass(desc);
        } else {
            try {
                return Module.loadClass(id, desc.getName());
            } catch (ModuleLoadException e) {
                throw new ClassNotFoundException("Failed to load module for class " + desc.getName(), e);
            }
        }
    }

    protected Class<?> resolveProxyClass(final String[] interfaces) throws IOException, ClassNotFoundException {
        ModuleIdentifier id = (ModuleIdentifier) readObject();
        if (id == null) {
            return super.resolveProxyClass(interfaces);
        } else {
            try {
                final ModuleClassLoader classLoader = ModuleClassLoader.forModule(id);
                final Class<?>[] actualInterfaces = new Class<?>[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    String name = interfaces[i];
                    actualInterfaces[i] = Class.forName(name, false, classLoader);
                }
                return Proxy.getProxyClass(classLoader, actualInterfaces);
            } catch (ModuleLoadException e) {
                throw new ClassNotFoundException("Failed to load module with ID " + id, e);
            }
        }
    }
}
