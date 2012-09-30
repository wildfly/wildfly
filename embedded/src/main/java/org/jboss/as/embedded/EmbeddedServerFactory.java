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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.log.JDKModuleLogger;

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
 * @author <a href="mailto:mmatloka@gmail.com">Michal Matloka</a>
 */
public class EmbeddedServerFactory {

    private EmbeddedServerFactory() {
    }

    public static StandaloneServer create(final ModuleLoader moduleLoader, final File jbossHomeDir, final Properties systemProps, final Map<String, String> systemEnv) {
        try {
            // Load the server Module and get its ClassLoader
            final ModuleIdentifier serverModuleId = ModuleIdentifier.create("org.jboss.as.server");
            final Module serverModule = moduleLoader.loadModule(serverModuleId);
            final ModuleClassLoader serverModuleClassLoader = serverModule.getClassLoader();

            Class<?> embeddedStandAloneServerFactoryClass = serverModuleClassLoader.loadClass("org.jboss.as.server.EmbeddedStandAloneServerFactory");
            Class<?> standaloneServerClass = serverModuleClassLoader.loadClass("org.jboss.as.embedded.StandaloneServer");
            Method createMethod = embeddedStandAloneServerFactoryClass.getMethod("create", File.class, ModuleLoader.class, Properties.class, Map.class);

            // Cast to StandaloneServer is not possible, same classes loaded by different classloaders are considered as
            // completely different. It is required to start and stop server via reflections
            final Object standaloneServer = createMethod.invoke(null, jbossHomeDir, moduleLoader, systemProps, systemEnv);
            Method startMethod = standaloneServerClass.getMethod("start", (Class<?>[]) null);
            Method stopMethod = standaloneServerClass.getMethod("stop", (Class<?>[]) null);

            return new StandaloneServerAdapter(standaloneServer, startMethod, stopMethod);
        } catch (ModuleLoadException e) {
            throw MESSAGES.moduleLoaderError(e, e.getMessage(), moduleLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static StandaloneServer create(final File jbossHomeDir, final Properties systemProps, final Map<String, String> systemEnv, String...systemPackages) {
        if (jbossHomeDir == null || !jbossHomeDir.isDirectory()) {
            throw MESSAGES.invalidJbossHome(jbossHomeDir);
        }
        if (systemProps.getProperty(ServerEnvironment.HOME_DIR) == null) {
            systemProps.setProperty(ServerEnvironment.HOME_DIR, jbossHomeDir.getAbsolutePath());
        }

        File modulesDir = new File(jbossHomeDir + "/modules");
        final ModuleLoader moduleLoader = InitialModuleLoaderFactory.getModuleLoader(modulesDir, systemPackages);

        try {
            Module.registerURLStreamHandlerFactoryModule(moduleLoader.loadModule(ModuleIdentifier.create("org.jboss.vfs")));

            // Initialize the Logging system
            ModuleIdentifier logModuleId = ModuleIdentifier.create("org.jboss.logmanager");
            ModuleClassLoader logModuleClassLoader = moduleLoader.loadModule(logModuleId).getClassLoader();
            ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(logModuleClassLoader);
                systemProps.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
                if (LogManager.getLogManager().getClass() == LogManager.class) {
                    System.err.println(MESSAGES.failedToLoadLogModule(logModuleId));
                } else {
                    Module.setModuleLogger(new JDKModuleLogger());
                }
            } finally {
                Thread.currentThread().setContextClassLoader(ctxClassLoader);
            }

            __redirected.__JAXPRedirected.changeAll(ModuleIdentifier.fromString("javax.xml.jaxp-provider"), moduleLoader);

            return create(moduleLoader, jbossHomeDir, systemProps, systemEnv);
        }
        catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public static void main(String[] args) throws Throwable {
        SecurityActions.setSystemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        String jbossHomeKey = "jboss.home";
        String jbossHomeProp = System.getProperty(jbossHomeKey);
        if (jbossHomeProp == null) {
            throw MESSAGES.systemPropertyNotFound(jbossHomeKey);
        }

        File jbossHomeDir = new File(jbossHomeProp);
        if (!jbossHomeDir.isDirectory()) {
            throw MESSAGES.invalidJbossHome(jbossHomeDir);
        }

        Module.registerURLStreamHandlerFactoryModule(Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.vfs")));
        StandaloneServer server = create(jbossHomeDir, System.getProperties(), System.getenv());

        server.start();
        server.stop();
        System.exit(0);
    }
}
