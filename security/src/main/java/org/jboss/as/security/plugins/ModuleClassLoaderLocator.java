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
package org.jboss.as.security.plugins;

import java.io.InputStream;
import java.net.URL;
import java.security.SecureClassLoader;

import org.jboss.as.security.SecurityMessages;
import org.jboss.modules.ModuleLoadException;
import org.jboss.security.plugins.ClassLoaderLocator;

/**
 * An implementation of {@code ClassLoaderLocator} that is based on JBoss Modules
 * @author anil saldhana
 */
public class ModuleClassLoaderLocator implements ClassLoaderLocator {
    @Override
    public ClassLoader get(String key) {
        try {
            ClassLoader moduleClassLoader = SecurityActions.getModuleClassLoader(key);
            ClassLoader tccl = SecurityActions.getContextClassLoader();
            /**
             * A Login Module can be in a custom user module.
             * The local resources (such as users.properties) can be present in a web deployment,
             * whose CL is available on the TCCL.
             */
            return new CombinedClassLoader(moduleClassLoader, tccl);
        } catch (ModuleLoadException e) {
            throw SecurityMessages.MESSAGES.runtimeException(e);
        }
    }
    /** A Classloader that takes in two Classloaders to delegate to */
    public class CombinedClassLoader extends SecureClassLoader{
        private ClassLoader first;
        private ClassLoader second;

        public CombinedClassLoader(ClassLoader firstCL, ClassLoader secondCL){
            this.first = firstCL;
            this.second = secondCL;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class<?> theClass = null;
            try {
                theClass = first.loadClass(name);
            } catch(ClassNotFoundException ce){
                theClass = second.loadClass(name);
            }

            return theClass;
        }

        @Override
        public URL getResource(String name) {
            URL resource = null;
            resource = first.getResource(name);
            if(resource == null){
                resource = second.getResource(name);
            }
            return resource;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            InputStream is = null;
            is = first.getResourceAsStream(name);
            if(is == null){
                is = second.getResourceAsStream(name);
            }
            return is;
        }
    }
}