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
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.log.JDKModuleLogger;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

/**
 * <p>
 * ServerFactory that sets up a standalone server using modular classloading.
 * </p>
 * <p>
 * To use this class the <code>jboss.home.dir</code> system property must be set to the
 * application server home directory. By default it will use the directories
 * <code>{$jboss.home.dir}/standalone/config</code> as the <i>configuration</i> directory and
 * <code>{$jboss.home.dir}/standalone/data</code> as the <i>data</i> directory. This can be overridden
 * with the <code>${jboss.server.base.dir}</code>, <code>${jboss.server.config.dir}</code> or <code>${jboss.server.config.dir}</code>
 * system properties as for normal server startup.
 * </p>
 * <p>
 * If a clean run is wanted, you can specify <code>${jboss.embedded.root}</code> to an existing directory
 * which will copy the contents of the data and configuration directories under a temporary folder. This
 * has the effect of this run not polluting later runs of the embedded server.
 * </p>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Thomas.Diesler@jboss.com
 */
public class EmbeddedServerFactory {

    private static final String MODULE_ID_EMBEDDED = "org.jboss.as.embedded";
    private static final String MODULE_ID_LOGMANAGER = "org.jboss.logmanager";
    private static final String MODULE_ID_VFS = "org.jboss.vfs";
    private static final String SYSPROP_KEY_CLASS_PATH = "java.class.path";
    private static final String SYSPROP_KEY_MODULE_PATH = "module.path";
    private static final String SYSPROP_KEY_BUNDLE_PATH = "jboss.bundles.dir";
    private static final String SYSPROP_KEY_LOGMANAGER = "java.util.logging.manager";
    private static final String SYSPROP_KEY_JBOSS_HOME_DIR = "jboss.home.dir";
    private static final String SYSPROP_VALUE_JBOSS_LOGMANAGER = "org.jboss.logmanager.LogManager";

    private EmbeddedServerFactory() {
    }

    public static StandaloneServer create(String jbossHomePath, String modulePath, String bundlePath, String... systemPackages) {
        if (jbossHomePath == null || jbossHomePath.isEmpty()) {
            throw MESSAGES.invalidJbossHome(jbossHomePath);
        }
        File jbossHomeDir = new File(jbossHomePath);
        if (!jbossHomeDir.isDirectory()) {
            throw MESSAGES.invalidJbossHome(jbossHomePath);
        }

        if (modulePath == null)
            modulePath = jbossHomeDir.getAbsolutePath() + File.separator + "modules";
        if (bundlePath == null)
            bundlePath = jbossHomeDir.getAbsolutePath() + File.separator + "bundles";

        setupBundlePath(bundlePath);

        return create(setupModuleLoader(modulePath, systemPackages), jbossHomeDir);
    }

    public static StandaloneServer create(ModuleLoader moduleLoader, File jbossHomeDir) {

        setupVfsModule(moduleLoader);
        setupLoggingSystem(moduleLoader);

        // Embedded Server wants this, too. Seems redundant, but supply it.
        SecurityActions.setSystemProperty(SYSPROP_KEY_JBOSS_HOME_DIR, jbossHomeDir.getAbsolutePath());

        // Load the Embedded Server Module
        final Module embeddedModule;
        try {
            embeddedModule = moduleLoader.loadModule(ModuleIdentifier.create(MODULE_ID_EMBEDDED));
        } catch (final ModuleLoadException mle) {
            throw new RuntimeException("Could not load the Embedded Server module", mle);
        }

        // Load the Embedded Server Factory via the modular environment
        final ModuleClassLoader embeddedModuleCL = embeddedModule.getClassLoader();
        final Class<?> embeddedServerFactoryClass;
        try {
            embeddedServerFactoryClass = embeddedModuleCL.loadClass(EmbeddedStandAloneServerFactory.class.getName());
        } catch (final ClassNotFoundException cnfe) {
            throw new RuntimeException(
                    "Could not load the embedded server factory from the embedded server module; check dependencies", cnfe);
        }

        // Get a handle to the method which will create the server
        final Method createServerMethod;
        try {
            createServerMethod = embeddedServerFactoryClass.getMethod("create", File.class, ModuleLoader.class, Properties.class, Map.class);
        } catch (final NoSuchMethodException nsme) {
            throw new RuntimeException("Could not get a handle to the method which will create the server", nsme);
        }

        // Create the server
        Object standaloneServer;
        try {
            standaloneServer = createServerMethod.invoke(null, jbossHomeDir, moduleLoader, SecurityActions.getSystemProperties(), SecurityActions.getSystemEnvironment());
        } catch (final InvocationTargetException ite) {
            throw new RuntimeException(ite);
        } catch (final IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
        return new StandaloneServerIndirection(standaloneServer);
    }

    private static ModuleLoader setupModuleLoader(String modulePath, String... systemPackages) {
        if (modulePath == null || modulePath.isEmpty()) {
            throw new IllegalStateException("Module path must be defined in the configuration");
        }

        final File modulesDir = new File(modulePath);
        if (!modulesDir.isDirectory()) {
            throw new IllegalStateException("Invalid modules directory: " + modulesDir);
        }

        final String classPath = SecurityActions.getSystemProperty(SYSPROP_KEY_CLASS_PATH);
        try {
            // Set up sysprop env
            SecurityActions.clearSystemProperty(SYSPROP_KEY_CLASS_PATH);
            SecurityActions.setSystemProperty(SYSPROP_KEY_MODULE_PATH, modulesDir.getAbsolutePath());

            StringBuffer packages = new StringBuffer("org.jboss.modules,org.jboss.msc,org.jboss.dmr,org.jboss.threads,org.jboss.as.controller.client");
            if (systemPackages != null) {
                for (String packageName : systemPackages)
                    packages.append("," + packageName);
            }
            SecurityActions.setSystemProperty("jboss.modules.system.pkgs", packages.toString());

            // Get the module loader
            final ModuleLoader moduleLoader = Module.getBootModuleLoader();
            return moduleLoader;
        } finally {
            // Return to previous state for classpath prop
            SecurityActions.setSystemProperty(SYSPROP_KEY_CLASS_PATH, classPath);
        }
    }

    private static void setupBundlePath(final String bundlePath) {
        if (bundlePath == null || bundlePath.isEmpty()) {
            throw new IllegalStateException("Bundle path must be defined in the configuration");
        }
        final File bundlesDir = new File(bundlePath);
        if (!bundlesDir.isDirectory()) {
            throw new IllegalStateException("Invalid bundles directory: " + bundlesDir);
        }
        SecurityActions.setSystemProperty(SYSPROP_KEY_BUNDLE_PATH, bundlePath);
    }

    private static void setupVfsModule(final ModuleLoader moduleLoader) {
        final ModuleIdentifier vfsModuleID = ModuleIdentifier.create(MODULE_ID_VFS);
        final Module vfsModule;
        try {
            vfsModule = moduleLoader.loadModule(vfsModuleID);
        } catch (final ModuleLoadException mle) {
            throw new RuntimeException("Could not load VFS module", mle);
        }
        Module.registerURLStreamHandlerFactoryModule(vfsModule);
    }

    private static void setupLoggingSystem(ModuleLoader moduleLoader) {
        final ModuleIdentifier logModuleId = ModuleIdentifier.create(MODULE_ID_LOGMANAGER);
        final Module logModule;
        try {
            logModule = moduleLoader.loadModule(logModuleId);
        } catch (final ModuleLoadException mle) {
            throw new RuntimeException("Could not load logging module", mle);
        }

        final ModuleClassLoader logModuleClassLoader = logModule.getClassLoader();
        final ClassLoader tccl = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(logModuleClassLoader);
            SecurityActions.setSystemProperty(SYSPROP_KEY_LOGMANAGER, SYSPROP_VALUE_JBOSS_LOGMANAGER);

            final Class<?> actualLogManagerClass = LogManager.getLogManager().getClass();
            if (actualLogManagerClass == LogManager.class) {
                System.err.println("Cannot not load JBoss LogManager. The LogManager has likely been accessed prior to this initialization.");
            } else {
                Module.setModuleLogger(new JDKModuleLogger());
            }
        } finally {
            // Reset TCCL
            SecurityActions.setContextClassLoader(tccl);
        }
    }
}
