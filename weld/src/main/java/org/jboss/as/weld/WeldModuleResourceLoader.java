/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld;

import org.jboss.modules.Module;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoadingException;

import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link ResourceLoader} that can load classes from a {@link Module}
 * <p>
 * Thread Safety: This class is thread safe, and does not require a happens before even between construction and usage
 *
 * @author Stuart Douglas
 *
 */
public class WeldModuleResourceLoader implements ResourceLoader {

    private final Module module;

    /**
     * Additional classes that have been added to the bean archive by the container or by a portable extension
     */
    private final Map<String, Class<?>> classes;

    public WeldModuleResourceLoader(Module module) {
        this.module = module;
        this.classes = new ConcurrentHashMap<String, Class<?>>();
    }

    /**
     * If the class name is found in additionalClasses then return it.
     *
     * Otherwise the class will be loaded from the module ClassLoader
     */
    @Override
    public Class<?> classForName(String name) {
        try {
            if (classes.containsKey(name)) {
                return classes.get(name);
            }
            final Class<?> clazz = module.getClassLoader().loadClass(name);
            classes.put(name, clazz);
            return clazz;
        } catch (ClassNotFoundException | LinkageError e) {
            throw new ResourceLoadingException(e);
        }
    }

    public void addAdditionalClass(Class<?> clazz) {
        this.classes.put(clazz.getName(), clazz);
    }

    /**
     * Loads a resource from the module class loader
     */
    @Override
    public URL getResource(String name) {
        try {
            return module.getClassLoader().getResource(name);
        } catch (Exception e) {
            throw new ResourceLoadingException(e);
        }
    }

    /**
     * Loads resources from the module class loader
     */
    @Override
    public Collection<URL> getResources(String name) {
        try {
            final HashSet<URL> resources = new HashSet<URL>();
            Enumeration<URL> urls = module.getClassLoader().getResources(name);
            while (urls.hasMoreElements()) {
                resources.add(urls.nextElement());
            }
            return resources;
        } catch (Exception e) {
            throw new ResourceLoadingException(e);
        }

    }

    @Override
    public void cleanup() {
        // nop
    }

}
