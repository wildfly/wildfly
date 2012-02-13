/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering;

import java.io.IOException;

import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;

/**
 * ClassResolver decorator with a simple class loader attachment.
 * Used for AS7-2496 workaround
 * @author Paul Ferraro
 */
public class ClassLoaderAwareClassResolver implements ClassResolver, ClassLoaderProvider {

    private final ClassResolver resolver;
    private final ClassLoader loader;

    public ClassLoaderAwareClassResolver(ClassResolver resolver, ClassLoader loader) {
        this.resolver = resolver;
        this.loader = loader;
    }

    public ClassLoader getClassLoader() {
        return this.loader;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.ClassResolver#annotateClass(org.jboss.marshalling.Marshaller, java.lang.Class)
     */
    @Override
    public void annotateClass(Marshaller marshaller, Class<?> clazz) throws IOException {
        this.resolver.annotateClass(marshaller, clazz);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.ClassResolver#annotateProxyClass(org.jboss.marshalling.Marshaller, java.lang.Class)
     */
    @Override
    public void annotateProxyClass(Marshaller marshaller, Class<?> proxyClass) throws IOException {
        this.resolver.annotateProxyClass(marshaller, proxyClass);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.ClassResolver#getClassName(java.lang.Class)
     */
    @Override
    public String getClassName(Class<?> clazz) throws IOException {
        return this.resolver.getClassName(clazz);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.ClassResolver#getProxyInterfaces(java.lang.Class)
     */
    @Override
    public String[] getProxyInterfaces(Class<?> proxyClass) throws IOException {
        return this.resolver.getProxyInterfaces(proxyClass);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.ClassResolver#resolveClass(org.jboss.marshalling.Unmarshaller, java.lang.String, long)
     */
    @Override
    public Class<?> resolveClass(Unmarshaller unmarshaller, String name, long serialVersionUID) throws IOException, ClassNotFoundException {
        return this.resolver.resolveClass(unmarshaller, name, serialVersionUID);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.ClassResolver#resolveProxyClass(org.jboss.marshalling.Unmarshaller, java.lang.String[])
     */
    @Override
    public Class<?> resolveProxyClass(Unmarshaller unmarshaller, String[] interfaces) throws IOException, ClassNotFoundException {
        return this.resolver.resolveProxyClass(unmarshaller, interfaces);
    }
}