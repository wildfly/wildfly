/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.embedded;

import static org.jboss.as.embedded.EmbeddedMessages.MESSAGES;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

import java.io.File;

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
     * @param systemPackages Packages that are exported by the SYSTEM module
     * @return The default module loader
     */
    public static ModuleLoader getModuleLoader(File modulePath, String... systemPackages) {
        if (modulePath == null)
            throw MESSAGES.nullVar("modulePath");
        if (modulePath.isDirectory() == false)
            throw MESSAGES.invalidModulePath(modulePath.getAbsolutePath());

        String oldClassPath = SecurityActions.getSystemProperty("java.class.path");
        try {
            SecurityActions.clearSystemProperty("java.class.path");
            SecurityActions.setSystemProperty("module.path", modulePath.getAbsolutePath());

            StringBuffer packages = new StringBuffer("org.jboss.modules," + InitialModuleLoaderFactory.class.getPackage().getName());
            // for model operations
            packages.append(",org.jboss.as.controller.client,org.jboss.dmr");
            if (systemPackages != null) {
                for (String packageName : systemPackages)
                    packages.append("," + packageName);
            }
            SecurityActions.setSystemProperty("jboss.modules.system.pkgs", packages.toString());

            ModuleLoader moduleLoader = Module.getBootModuleLoader();

            return moduleLoader;
        } finally {
            SecurityActions.setSystemProperty("java.class.path", oldClassPath);
        }
    }
}
