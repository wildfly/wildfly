/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.embedded;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.LogManager;

import javax.naming.Context;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.log.JDKModuleLogger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Indirection to the {@link StandaloneServer}; used to encapsulate access to the underlying embedded AS Server instance in a
 * manner that does not directly link this class. Necessary to avoid {@link ClassCastException} when this class is loaded by the
 * application {@link ClassLoader} (or any other hierarchical CL) while the server is loaded by a modular environment.
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @author Thomas.Diesler@jboss.com
 */
public final class StandaloneServerIndirection implements StandaloneServer {

    private static final String MODULE_ID_EMBEDDED = "org.jboss.as.embedded";
    private static final String MODULE_ID_LOGMANAGER = "org.jboss.logmanager";
    private static final String MODULE_ID_VFS = "org.jboss.vfs";
    private static final String SYSPROP_KEY_CLASS_PATH = "java.class.path";
    private static final String SYSPROP_KEY_MODULE_PATH = "module.path";
    private static final String SYSPROP_KEY_BUNDLE_PATH = "jboss.bundles.dir";
    private static final String SYSPROP_KEY_LOGMANAGER = "java.util.logging.manager";
    private static final String SYSPROP_KEY_JBOSS_HOME_DIR = "jboss.home.dir";
    private static final String SYSPROP_VALUE_JBOSS_LOGMANAGER = "org.jboss.logmanager.LogManager";

    private final Object standaloneServer;
    private final Method methodStart;
    private final Method methodStop;
    private final Method methodGetService;
    private final Method methodGetModelControllerClient;

    public static StandaloneServer create(String jbossHomePath, String modulePath, String bundlePath, String... systemPackages) {
        if (jbossHomePath == null || jbossHomePath.isEmpty()) {
            throw new IllegalStateException("JBoss home path must be defined");
        }
        File jbossHome = new File(jbossHomePath);
        if (!jbossHome.isDirectory()) {
            throw new IllegalStateException("Invalid jboss home directory: " + jbossHome);
        }

        if (modulePath == null)
            modulePath = jbossHome.getAbsolutePath() + File.separator + "modules";
        if (bundlePath == null)
            bundlePath = jbossHome.getAbsolutePath() + File.separator + "bundles";

        ModuleLoader moduleLoader = setupModuleLoader(modulePath, systemPackages);
        setupBundlePath(bundlePath);
        setupVfsModule(moduleLoader);
        setupLoggingSystem(moduleLoader);

        // Embedded Server wants this, too. Seems redundant, but supply it.
        SecurityActions.setSystemProperty(SYSPROP_KEY_JBOSS_HOME_DIR, jbossHome.getAbsolutePath());
        return new StandaloneServerIndirection(moduleLoader, jbossHome);
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

    private StandaloneServerIndirection(final ModuleLoader loader, final File jbossHome) throws IllegalArgumentException {
        if (loader == null) {
            throw new IllegalArgumentException("loader must be specified");
        }
        if (jbossHome == null) {
            throw new IllegalArgumentException("JBOSS_HOME must be specified");
        }
        if (!jbossHome.exists()) {
            throw new IllegalArgumentException("JBOSS_HOME must exist: " + jbossHome.getAbsolutePath());
        }
        if (!jbossHome.isDirectory()) {
            throw new IllegalArgumentException("JBOSS_HOME must be a directory: " + jbossHome.getAbsolutePath());
        }

        // Load the Embedded Server Module
        final Module embeddedModule;
        try {
            embeddedModule = loader.loadModule(ModuleIdentifier.create(MODULE_ID_EMBEDDED));
        } catch (final ModuleLoadException mle) {
            throw new RuntimeException("Could not load the Embedded Server module", mle);
        }

        // Load the Embedded Server Factory via the modular environment
        final ModuleClassLoader embeddedModuleCL = embeddedModule.getClassLoader();
        final Class<?> embeddedServerFactoryClass;
        final Class<?> standaloneServerClass;
        try {
            embeddedServerFactoryClass = embeddedModuleCL.loadClass(EmbeddedServerFactory.class.getName());
            standaloneServerClass = embeddedModuleCL.loadClass(StandaloneServer.class.getName());
        } catch (final ClassNotFoundException cnfe) {
            throw new RuntimeException(
                    "Could not load the embedded server factory from the embedded server module; check dependencies", cnfe);
        }

        // Get a handle to the method which will create the server
        final Method createServerMethod;
        try {
            createServerMethod = embeddedServerFactoryClass.getMethod("create", ModuleLoader.class, File.class, Properties.class, Map.class);
        } catch (final NoSuchMethodException nsme) {
            throw new RuntimeException("Could not get a handle to the method which will create the server", nsme);
        }

        // Create the server
        try {
            standaloneServer = createServerMethod.invoke(null, loader, jbossHome, SecurityActions.getSystemProperties(), SecurityActions.getSystemEnvironment());
        } catch (final InvocationTargetException ite) {
            throw new RuntimeException(ite);
        } catch (final IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }

        // Get a handle on the method
        try {
            methodStart = standaloneServerClass.getMethod("start");
            methodStop = standaloneServerClass.getMethod("stop");
            methodGetService = standaloneServerClass.getMethod("getService", ServiceName.class);
            methodGetModelControllerClient = standaloneServerClass.getMethod("getModelControllerClient");
        } catch (final NoSuchMethodException nsme) {
            throw new RuntimeException("Could not get a handle to the method to invoke upon the server", nsme);
        }
    }

    @Override
    public void start()  {
        invokeOnServer(methodStart);
    }

    @Override
    public void stop()  {
        invokeOnServer(methodStop);
    }

    @Override
    public ServiceController<?> getService(ServiceName serviceName) {
        return (ServiceController<?>) invokeOnServer(methodGetService, serviceName);
    }

    @Override
    public ModelControllerClient getModelControllerClient()  {
        return (ModelControllerClient) invokeOnServer(methodGetModelControllerClient);
    }

    @Override
    public void deploy(File file)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public void undeploy(File file) throws ExecutionException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context getContext() {
        throw new UnsupportedOperationException();
    }

    private Object invokeOnServer(final Method method, Object... args) {
        try {
            return method.invoke(standaloneServer, args);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            Throwable cause = ex;
            if (ex instanceof InvocationTargetException) {
                cause = ((InvocationTargetException)ex).getCause();
            }
            throw new IllegalStateException(cause);
        }
    }
}
