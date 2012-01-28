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

package org.jboss.as.jpa.classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.jboss.modules.ConcurrentClassLoader;

/**
 * Return a new instance of a ClassLoader that the may be used to temporarily load any classes,
 * resources, or open URLs.  None of the classes loaded by this class loader will be visible to
 * application components.
 * <p/>
 * TempClassLoader is suitable for implementing javax.persistence.spi.PersistenceUnitInfo.getNewTempClassLoader()
 * <p/>
 *
 * @author Scott Marlow
 * @author Antti Laisi
 */
public class TempClassLoader extends ConcurrentClassLoader {

    private final ClassLoader delegate;

    TempClassLoader(final ClassLoader delegate) {
        super(null);
        this.delegate = delegate;
    }

    @Override
    protected Class<?> findClass(String name, boolean exportsOnly, boolean resolve) throws ClassNotFoundException {

        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }

        // javax.persistence classes must be loaded by module classloader, otherwise
        // the persistence provider can't read JPA annotations with reflection
        if (name.startsWith("javax.")) {
            return Class.forName(name, resolve, delegate);
        }

        InputStream resource = delegate.getResourceAsStream(name.replace('.', '/') + ".class");
        if (resource == null) {
            throw new ClassNotFoundException(name);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            for (int i = 0; (i = resource.read(buffer, 0, buffer.length)) != -1; ) {
                baos.write(buffer, 0, i);
            }
            buffer = baos.toByteArray();
            return defineClass(name, buffer, 0, buffer.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        } finally {
            try {
                resource.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    protected URL findResource(String name, boolean exportsOnly) {
        return delegate.getResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name, boolean exportsOnly) throws IOException {
        return delegate.getResources(name);
    }

    @Override
    protected InputStream findResourceAsStream(String name, boolean exportsOnly) {
        return delegate.getResourceAsStream(name);
    }

}
