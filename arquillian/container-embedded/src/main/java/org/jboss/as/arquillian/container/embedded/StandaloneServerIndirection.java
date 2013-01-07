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
package org.jboss.as.arquillian.container.embedded;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.as.embedded.EmbeddedServerFactory;
import org.jboss.as.embedded.StandaloneServer;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * Indirection to the {@link StandaloneServer}; used to encapsulate access to the underlying embedded AS Server instance in a
 * manner that does not directly link this class. Necessary to avoid {@link ClassCastException} when this class is loaded by the
 * application {@link ClassLoader} (or any other hierarchical CL) while the server is loaded by a modular environment.
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
class StandaloneServerIndirection {

    private static final String MODULE_ID_EMBEDDED_CANONICAL_FORM = "org.jboss.as.embedded";

    private static final ModuleIdentifier MODULE_ID_EMBEDDED = ModuleIdentifier.create(MODULE_ID_EMBEDDED_CANONICAL_FORM);

    private static final String METHOD_NAME_CREATE = "create";

    private static final String METHOD_NAME_START = "start";

    private static final String METHOD_NAME_STOP = "stop";

    /**
     * Instance of {@link StandaloneServer}; represented as {@link Object} to avoid leaking a direct link from this class
     */
    private final Object standaloneServer;

    /**
     * Creates a new instance with the specified, required {@link ModuleLoader}, which will be used to load and create the
     * server located at the specified, required JBOSS_HOME.
     *
     * @param loader
     * @param jbossHome Location of AS installation local filesystem
     * @throws IllegalArgumentException If the {@link ModuleLoader} is not specified or if <code>JBOSS_HOME</code> is not
     *         specified, not a directory, or doesn't exist
     */
    StandaloneServerIndirection(final ModuleLoader loader, final File jbossHome) throws IllegalArgumentException {
        // Precondition checks
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
        final ModuleIdentifier embeddedModuleId = MODULE_ID_EMBEDDED;
        final Module embeddedModule;
        try {
            embeddedModule = loader.loadModule(embeddedModuleId);
        } catch (final ModuleLoadException mle) {
            throw new RuntimeException("Could not load the Embedded Server module", mle);
        }

        // Load the Embedded Server Factory via the modular environment
        final ModuleClassLoader embeddedModuleCL = embeddedModule.getClassLoader();
        final Class<?> embeddedServerFactoryClass;
        try {
            embeddedServerFactoryClass = embeddedModuleCL.loadClass(EmbeddedServerFactory.class.getName());
        } catch (final ClassNotFoundException cnfe) {
            throw new RuntimeException(
                    "Could not load the embedded server factory from the embedded server module; check dependencies", cnfe);
        }

        // Get a handle to the method which will create the server
        final Method createServerMethod;
        try {
            createServerMethod = embeddedServerFactoryClass.getMethod(METHOD_NAME_CREATE, ModuleLoader.class, File.class,
                    Properties.class, Map.class);
        } catch (final NoSuchMethodException nsme) {
            throw new RuntimeException("Could not get a handle to the method which will create the server", nsme);
        }

        // Create the server
        final Object standaloneServer;
        try {
            standaloneServer = createServerMethod.invoke(null, loader, jbossHome, SecurityActions.getSystemProperties(),
                    SecurityActions.getSystemEnvironment());
        } catch (final InvocationTargetException ite) {
            throw new RuntimeException(ite);
        } catch (final IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }

        // Set
        this.standaloneServer = standaloneServer;
    }

    /**
     * Starts the server
     *
     * @throws LifecycleException
     */
    void start() throws LifecycleException {
        this.invokeOnServer(METHOD_NAME_START);
    }

    /**
     * Stops the server
     *
     * @throws LifecycleException
     */
    void stop() throws LifecycleException {
        this.invokeOnServer(METHOD_NAME_STOP);
    }

    /**
     * Invokes the method of the specified name on the backing server
     *
     * @throws LifecycleException Returned and wraps any {@link Exception} encountered during invocation
     */
    private void invokeOnServer(final String methodName) throws LifecycleException {
        // Precondition checks
        if (methodName == null || methodName.length() == 0) {
            throw new IllegalArgumentException("method name must be specified");
        }

        // Get a handle on the method
        final Class<?> standaloneServerClass = standaloneServer.getClass();
        final Method method;
        try {
            method = standaloneServerClass.getMethod(methodName);
        } catch (final NoSuchMethodException nsme) {
            throw new RuntimeException("Could not get a handle to the method to invoke upon the server, \"" + methodName
                    + "\", of " + standaloneServer, nsme);
        }

        // Set accessible
        SecurityActions.setAccessible(method);

        // Invoke
        try {
            method.invoke(standaloneServer);
        } catch (final Exception e) {
            throw new LifecycleException("Could not invoke " + methodName + " on: " + standaloneServer, e);
        }
    }

}
