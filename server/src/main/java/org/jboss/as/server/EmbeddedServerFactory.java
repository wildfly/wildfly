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
package org.jboss.as.server;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;
import java.util.logging.LogManager;

import org.jboss.modules.JDKModuleLogger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * ServerFactory that sets up a standalone server using modular classloading
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class EmbeddedServerFactory {
    private EmbeddedServerFactory() {
    }

    public static StandaloneServer create(final File jbossHomeDir, final Properties systemProps) throws Throwable {

        if (jbossHomeDir == null || jbossHomeDir.isDirectory() == false)
            throw new IllegalStateException("Invalid jboss.home.dir: " + jbossHomeDir);

        if (systemProps.getProperty("jboss.home.dir") == null) {
            systemProps.setProperty("jboss.home.dir", jbossHomeDir.getAbsolutePath());
        }
        systemProps.setProperty("jboss.home.dir", jbossHomeDir.getAbsolutePath());

        File modulesDir = new File(jbossHomeDir + "/modules");
        ModuleLoader moduleLoader = InitialModuleLoaderFactory.getModuleLoader(modulesDir, "org.jboss.logmanager");

        // Initialize the Logging system
        ModuleIdentifier logModuleId = ModuleIdentifier.create("org.jboss.logmanager");
        ModuleClassLoader logModuleClassLoader = moduleLoader.loadModule(logModuleId).getClassLoader();
        ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(logModuleClassLoader);
            systemProps.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
            if (LogManager.getLogManager().getClass() == LogManager.class) {
                System.err.println("WARNING: Failed to load the specified logmodule " + logModuleId);
            } else {
                Module.setModuleLogger(new JDKModuleLogger());
            }
        } finally {
            Thread.currentThread().setContextClassLoader(ctxClassLoader);
        }

        // Load the server Module
        ModuleIdentifier serverModuleId = ModuleIdentifier.create("org.jboss.as.server");
        Module serverModule = moduleLoader.loadModule(serverModuleId);

        // Determine the ServerEnvironment
        ModuleClassLoader serverModuleClassLoader = serverModule.getClassLoader();
        Class<?> serverMainClass = serverModuleClassLoader.loadClass("org.jboss.as.server.Main");
        Method determineEnvironmentMethod = serverMainClass.getMethod("determineEnvironment", String[].class, Properties.class);
        Object serverEnvironment = determineEnvironmentMethod.invoke(null, new String[0], systemProps);

        // Get the StandaloneServer instance
        final Class<?> serverClass = serverModuleClassLoader.loadClass("org.jboss.as.server.StandaloneServer");
        Class<?> serverFactoryClass = serverModuleClassLoader.loadClass("org.jboss.as.server.StandaloneServerFactory");
        Method createMethod = serverFactoryClass.getMethod("create", serverEnvironment.getClass());
        final Object server = createMethod.invoke(null, serverEnvironment);

        InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Method targetMethod = serverClass.getMethod(method.getName(), method.getParameterTypes());
                return targetMethod.invoke(server, args);
            }
        };

        ClassLoader classLoader = StandaloneServer.class.getClassLoader();
        Class<?>[] interfaces = new Class[] { StandaloneServer.class };
        Object proxy = Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
        return (StandaloneServer) proxy;
    }

    public static void main(String[] args) throws Throwable {
        SecurityActions.setSystemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        String jbossHomeKey = "jboss.home";
        String jbossHomeProp = System.getProperty(jbossHomeKey);
        if (jbossHomeProp == null)
            throw new IllegalStateException("Cannot find system property: " + jbossHomeKey);

        File jbossHomeDir = new File(jbossHomeProp);
        if (jbossHomeDir.isDirectory() == false)
            throw new IllegalStateException("Invalid jboss home directory: " + jbossHomeDir);

        StandaloneServer server = create(jbossHomeDir, System.getProperties());
        server.start();
    }
}
