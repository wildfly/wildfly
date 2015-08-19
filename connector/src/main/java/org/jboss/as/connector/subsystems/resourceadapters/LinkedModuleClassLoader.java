/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.resourceadapters;

import static java.security.AccessController.doPrivileged;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.jboss.modules.ModuleClassLoader;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * LinkedModuleClassLoader is used to delegate class loading to other <code>org.jboss.modules.ModuleClassLoader</code>s.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
public class LinkedModuleClassLoader extends ClassLoader {

    private final Set<ModuleClassLoader> classLoaders;

    public static LinkedModuleClassLoader createClassLoader(final ModuleClassLoader parent, final Set<ModuleClassLoader> extensionClassLoaders) {
        return ! WildFlySecurityManager.isChecking() ?
                new LinkedModuleClassLoader(parent, extensionClassLoaders) :
                doPrivileged(new PrivilegedAction<LinkedModuleClassLoader>() {
                    @Override
                    public LinkedModuleClassLoader run() {
                        return new LinkedModuleClassLoader(parent, extensionClassLoaders);
                    }
                });
    }

    /**
     * create an instance.
     *
     * @param parent, the parent ClassLoader, default to Caller ModuleClassLoader.
     * @param classLoaders The extra class loaders to delegate.
     */
    private LinkedModuleClassLoader(ModuleClassLoader parent, Set<ModuleClassLoader> classLoaders) {
        super(parent);
        this.classLoaders = classLoaders;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> theClass = null;
        try {
            theClass = super.loadClass(name);
            return theClass;
        } catch(ClassNotFoundException ce){
            // ignore it
        }
        if (this.classLoaders != null) {
            for(ClassLoader cl: classLoaders) {
                try {
                    theClass = cl.loadClass(name);
                    return theClass;
                } catch(ClassNotFoundException ce){
                    // ignore it
                }
            }
        }
        throw new ClassNotFoundException("Can't load Class: " + name);
    }

    @Override
    public URL getResource(String name) {
        URL resource = super.getResource(name);
        if (resource != null) {
            return resource;
        }
        if (this.classLoaders != null) {
            for(ClassLoader cl: classLoaders) {
                if ((resource = cl.getResource(name)) != null) {
                    return resource;
                }
            }
        }
        return resource;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        if (is != null) {
            return is;
        }
        if (this.classLoaders != null) {
            for(ClassLoader cl: classLoaders) {
                if ((is = cl.getResourceAsStream(name)) != null) {
                    return is;
                }
            }
        }
        return is;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> combinedList = new ArrayList<URL>();
        combinedList.addAll(Collections.list(super.getResources(name)));
        if (this.classLoaders != null) {
            for(ClassLoader cl: classLoaders) {
                combinedList.addAll(Collections.list(cl.getResources(name)));
            }
        }
        return Collections.enumeration(combinedList);
    }

}