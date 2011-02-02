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

package org.jboss.as.ee.component;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.security.CodeSource;
import java.util.Collection;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.modules.ClassSpec;
import org.jboss.modules.PackageSpec;
import org.jboss.modules.Resource;
import org.jboss.modules.ResourceLoader;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class EEProxyResourceLoader implements ResourceLoader {
    private final CodeSource codeSource;
    private final ClassLoader classLoader;

    EEProxyResourceLoader(final CodeSource codeSource, final ClassLoader classLoader) {
        this.codeSource = codeSource;
        this.classLoader = classLoader;
    }

    public String getRootName() {
        return "$$ee$proxy";
    }

    public ClassSpec getClassSpec(final String fileName) throws IOException {
        if (fileName.endsWith("$$ee$proxy.class")) {
            final int fileNameLength = fileName.length();
            final String proxyClassName = fileName.substring(0, fileNameLength - 6).replace('/', '.');
            final String originalClassName = proxyClassName.substring(0, fileNameLength - 16);
            final Class<?> originalClass;
            try {
                originalClass = classLoader.loadClass(originalClassName);
            } catch (ClassNotFoundException e) {
                return null;
            }
            // skip final and inner classes
            if (Modifier.isFinal(originalClass.getModifiers()) || originalClass.getEnclosingClass() != null) {
                return null;
            }
            final ProxyFactory<?> proxyFactory;
            if (originalClass.isInterface()) {
                proxyFactory = createProxyFactory(proxyClassName, Object.class, originalClass);
            } else {
                proxyFactory = createProxyFactory(proxyClassName, originalClass);
            }
            final ClassSpec spec = new ClassSpec();
            // TODO: get class bytes from proxy factory
            //spec.setBytes(proxyFactory.getProxyBytes());
            spec.setCodeSource(codeSource);
            return spec;
        } else {
            return null;
        }
    }

    private static <T> ProxyFactory<T> createProxyFactory(String className, Class<T> superClass, Class<?>... interfaces) {
        return new ProxyFactory<T>(className, superClass, interfaces);
    }

    public PackageSpec getPackageSpec(final String name) throws IOException {
        return null;
    }

    public Resource getResource(final String name) {
        return null;
    }

    public String getLibrary(final String name) {
        return null;
    }

    public Collection<String> getPaths() {
        return null;
    }
}
