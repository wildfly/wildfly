/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.weld.deployment.processors;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link ClassLoader} that delegates loading of classes and resources to a list of delegate class loaders.
 * @author Paul Ferraro
 */
class CompositeClassLoader extends ClassLoader {
    private List<ClassLoader> loaders;

    CompositeClassLoader(List<ClassLoader> loaders) {
        super(null);
        this.loaders = loaders;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassLoader loader : this.loaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
        return super.findClass(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<Enumeration<URL>> results = new ArrayList<>(this.loaders.size());
        for (ClassLoader loader : this.loaders) {
            results.add(loader.getResources(name));
        }
        Iterator<Enumeration<URL>> iterator = results.iterator();
        return new Enumeration<>() {
            private Enumeration<URL> urls = Collections.emptyEnumeration();

            @Override
            public boolean hasMoreElements() {
                return this.getURLs().hasMoreElements();
            }

            @Override
            public URL nextElement() {
                return this.getURLs().nextElement();
            }

            private Enumeration<URL> getURLs() {
                if (!this.urls.hasMoreElements() && iterator.hasNext()) {
                    this.urls = iterator.next();
                }
                return this.urls;
            }
        };
    }

    @Override
    protected URL findResource(String name) {
        for (ClassLoader loader : this.loaders) {
            URL resource = loader.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return super.findResource(name);
    }
}
