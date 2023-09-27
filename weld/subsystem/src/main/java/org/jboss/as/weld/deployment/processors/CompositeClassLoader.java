/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
