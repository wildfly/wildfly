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

import org.jboss.modules.ConcurrentClassLoader;
import org.jboss.modules.ModuleClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Return a new instance of a ClassLoader that the may be used to temporarily load any classes,
 * resources, or open URLs.  None of the classes loaded by this class loader will be visible to
 * application components.
 * <p/>
 * TempClassLoader is suitable for implementing javax.persistence.spi.PersistenceUnitInfo.getNewTempClassLoader()
 * <p/>
 * TODO:  prove that loaded application classes aren't visible to PUI.getClassLoader()
 *
 * @author Scott Marlow
 */
public class TempClassLoader extends ConcurrentClassLoader {

    private final ModuleClassLoader delegate;

    public TempClassLoader(final ModuleClassLoader delegate) {
        this.delegate = delegate;
    }

    @Override
    protected URL findResource(String name, boolean exportsOnly) {
        return delegate.findResource(name, exportsOnly);
    }

    @Override
    protected Enumeration<URL> findResources(String name, boolean exportsOnly) throws IOException {
        return delegate.findResources(name, exportsOnly);
    }

    @Override
    protected InputStream findResourceAsStream(String name, boolean exportsOnly) {
        return super.findResourceAsStream(name, exportsOnly);
    }

    @Override
    protected Class<?> findClass(String className, boolean exportsOnly, boolean resolve) throws ClassNotFoundException {
        return super.findClass(className, exportsOnly, resolve);
    }
}
