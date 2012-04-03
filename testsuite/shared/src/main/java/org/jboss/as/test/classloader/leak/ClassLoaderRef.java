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
package org.jboss.as.test.classloader.leak;

import java.lang.ref.WeakReference;
import org.jboss.modules.Module;

/**
 *
 * Helper class holding weak reference to module classloader. Used for classloader leak tests.
 * 
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class ClassLoaderRef {    
    private static String moduleIdentifier;
    private static WeakReference<ClassLoader> classLoaderRef;
    private static WeakReference<Module> moduleRef;

    /**
     * @return the moduleIdentifier
     */
    public static String getModuleIdentifier() {
        return moduleIdentifier;
    }

    /**
     * @param aModuleIdentifier the moduleIdentifier to set
     */
    public static void setModuleIdentifier(String aModuleIdentifier) {
        moduleIdentifier = aModuleIdentifier;
    }

    /**
     * @return the classLoaderRef
     */
    public static WeakReference<ClassLoader> getClassLoaderRef() {
        return classLoaderRef;
    }

    /**
     * @param aClassLoaderRef the classLoaderRef to set
     */
    public static void setClassLoaderRef(WeakReference<ClassLoader> aClassLoaderRef) {
        classLoaderRef = aClassLoaderRef;
    }

    /**
     * @return the moduleRef
     */
    public static WeakReference<Module> getModuleRef() {
        return moduleRef;
    }

    /**
     * @param aModuleRef the moduleRef to set
     */
    public static void setModuleRef(WeakReference<Module> aModuleRef) {
        moduleRef = aModuleRef;
    }
}
