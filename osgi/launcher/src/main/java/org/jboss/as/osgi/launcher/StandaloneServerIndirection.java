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
package org.jboss.as.osgi.launcher;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
 * @author David Bosschaert
 */
class StandaloneServerIndirection {
    private static final ModuleIdentifier MODULE_ID_EMBEDDED = ModuleIdentifier.create("org.jboss.as.embedded");

    /**
     * Instance of {@link StandaloneServer}; represented as {@link Object} to avoid leaking a direct link from this class
     */
    private final Object standaloneServer;

    /**
     * Creates a new instance with the specified {@link ModuleLoader}.
     *
     * @param loader The Module Loader to use to create the embedded server.
     * @param jbossHome Location of AS installation local filesystem
     */
    StandaloneServerIndirection(final ModuleLoader loader, final File jbossHome) {
        // Load the Embedded Server Module
        ModuleIdentifier embeddedModuleId = MODULE_ID_EMBEDDED;
        Module embeddedModule;
        try {
            embeddedModule = loader.loadModule(embeddedModuleId);
        } catch (ModuleLoadException mle) {
            throw new RuntimeException(mle);
        }

        // Load the Embedded Server Factory via the modular environment
        ModuleClassLoader embeddedModuleCL = embeddedModule.getClassLoader();
        Class<?> embeddedServerFactoryClass;
        try {
            embeddedServerFactoryClass = embeddedModuleCL.loadClass(EmbeddedServerFactory.class.getName());
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }

        standaloneServer = invokeReflectively(embeddedServerFactoryClass, "create",
                Arrays.<Class<?>>asList(ModuleLoader.class, File.class, Properties.class, Map.class),
                Arrays.asList(loader, jbossHome, SecurityActions.getSystemProperties(), SecurityActions.getSystemEnvironment()));
    }

    /**
     * Reflectively use the DMR to activate the OSGi subsystem.
     */
    public void activateOSGiSubsystem() throws Exception {
        Object controller = invokeReflectively(standaloneServer, "getModelControllerClient");

        // Create a model node with the activate operation for the OSGi subsystem
        Class<?> modelNodeClass = controller.getClass().getClassLoader().loadClass("org.jboss.dmr.ModelNode");
        Object modelNode = modelNodeClass.newInstance();
        Object addressNode = invokeReflectively(modelNode, "get", String.class, "address");
        Object opList = invokeReflectively(addressNode, "setEmptyList");
        invokeReflectively(opList, "add", Arrays.<Class<?>>asList(String.class, String.class),
                                          Arrays.asList("subsystem", "osgi"));
        Object opNode = invokeReflectively(modelNode, "get", String.class, "operation");
        invokeReflectively(opNode, "set", String.class, "activate");

        // Execute it
        invokeReflectively(controller, "execute", modelNodeClass, modelNode);
    }

    /**
     * Retrieve an MSC service from the server and waits for the service to be active.
     *
     * @param timeout The amount to time (in milliseconds) to wait for the service
     * @param nameSegments The ServiceName segments
     * @return the service or null if not found
     */
    public Object getService(long timeout, String ... nameParts) {
        return invokeReflectively(standaloneServer, "getService",
                Arrays.<Class<?>>asList(long.class, String[].class),
                Arrays.asList(timeout, nameParts));
    }

    /**
     * Starts the server
     */
    void start() {
        invokeReflectively(standaloneServer, "start");
    }

    /**
     * Stops the server
     */
    void stop() {
        invokeReflectively(standaloneServer, "stop");
    }

    private static Object invokeReflectively(Object object, String methodName) {
        return invokeReflectively(object, methodName, Collections.<Class<?>>emptyList(), Collections.emptyList());
    }

    private static Object invokeReflectively(Object object, String methodName, Class<?> parameterType, Object param) {
        return invokeReflectively(object, methodName, Collections.<Class<?>>singletonList(parameterType), Collections.singletonList(param));
    }

    private static Object invokeReflectively(Class<?> clazz, String methodName, List<Class<?>> parameterTypes, List<? extends Object> params) {
        return invokeReflectively(null, clazz, methodName, parameterTypes, params);
    }

    private static Object invokeReflectively(Object object, String methodName, List<Class<?>> parameterTypes, List<? extends Object> params) {
        return invokeReflectively(object, object.getClass(), methodName, parameterTypes, params);
    }

    private static Object invokeReflectively(Object object, Class<?> clazz, String methodName, List<Class<?>> parameterTypes, List<? extends Object> params) {
        try {
            Method m = clazz.getMethod(methodName, parameterTypes.toArray(new Class[] {}));
            SecurityActions.setAccessible(m);
            return m.invoke(object, params.toArray());
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
