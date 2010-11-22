/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.server;

import java.io.File;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * A factory for the initial ModuleLoader.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
final class InitialModuleLoaderFactory {

    // Hide ctor
    private InitialModuleLoaderFactory() {
    }

    /**
     * Initializes the modules system and obtains the initial default ModuleLoader
     *
     * @param systemPaths Paths that are exported by the SYSTEM module
     * @return The default module loader
     */
    public static ModuleLoader getModuleLoader(File modulePath, String... systemPackages) {

        if (modulePath == null || modulePath.isDirectory() == false)
            throw new IllegalArgumentException("Invalid module path: " + modulePath);

        String oldClassPath = SecurityActions.getSystemProperty("java.class.path");
        try {
            SecurityActions.clearSystemProperty("java.class.path");
            SecurityActions.setSystemProperty("module.path", modulePath.getAbsolutePath());

            //These two are experimental in jboss-modules snapshot
            SecurityActions.setSystemProperty("module.include.path", "javax/transaction/InvalidTransactionException,javax/transaction/TransactionRequiredException,javax/transaction/TransactionRolledbackException");
            SecurityActions.setSystemProperty("module.exclude.path", "javax/transaction/**");

            StringBuffer packages = new StringBuffer("org.jboss.modules");
            if (systemPackages != null) {
                for (String packageName : systemPackages)
                    packages.append("," + packageName);
            }
            SecurityActions.setSystemProperty("jboss.modules.system.pkgs", packages.toString());

            ModuleLoader moduleLoader = Module.getDefaultModuleLoader();

            // Sanity check that the SYSTEM module ClassLoader cannot see this class
            try {
                ModuleClassLoader classLoader = moduleLoader.loadModule(ModuleIdentifier.SYSTEM).getClassLoader();
                classLoader.loadClass(InitialModuleLoaderFactory.class.getName());
                throw new IllegalStateException("Cannot initialize module system. There was probably a previous usage.");
            } catch (ModuleLoadException e) {
                // ignore
            } catch (ClassNotFoundException ex) {
                // expected
            }

            return moduleLoader;
        } finally {
            SecurityActions.setSystemProperty("java.class.path", oldClassPath);
        }
    }

    private static String classToResource(Class<?> clazz) {
        String base = clazz.getName().replace('.', '/');
        return base + ".class";
    }
}
