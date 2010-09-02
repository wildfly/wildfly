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
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.jboss.modules.Module;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ModularObjectOutputStream extends ObjectOutputStream {

    private ModularObjectOutputStream(final OutputStream out) throws IOException {
        super(out);
    }

    static ModularObjectOutputStream create(final OutputStream out) throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ModularObjectOutputStream>() {
                    public ModularObjectOutputStream run() throws IOException {
                        return new ModularObjectOutputStream(out);
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
            return new ModularObjectOutputStream(out);
        }
    }

    protected void annotateClass(final Class<?> cl) throws IOException {
        final Module module = Module.forClass(cl);
        if (module == null) {
            writeObject(null);
        } else {
            writeObject(module.getIdentifier());
        }
        return;
    }

    protected void annotateProxyClass(final Class<?> cl) throws IOException {
        // proxy interfaces must be visible from their CL, thus we only need to write
        // the module of the actual proxy class
        final Module module = Module.forClass(cl);
        if (module == null) {
            writeObject(null);
        } else {
            writeObject(module.getIdentifier());
        }
        return;
    }
}
