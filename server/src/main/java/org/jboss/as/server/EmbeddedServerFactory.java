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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

import org.jboss.as.protocol.StreamUtils;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.log.JDKModuleLogger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.threads.AsyncFuture;

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

    public static final String JBOSS_EMBEDDED_ROOT = "jboss.embedded.root";

    private EmbeddedServerFactory() {
    }

    public static StandaloneServer create(final File jbossHomeDir, final Properties systemProps, final Map<String, String> systemEnv, String...systemPackages) throws Throwable {

        if (jbossHomeDir == null || jbossHomeDir.isDirectory() == false)
            throw new IllegalStateException("Invalid jboss.home.dir: " + jbossHomeDir);

        if (systemProps.getProperty(ServerEnvironment.HOME_DIR) == null)
            systemProps.setProperty(ServerEnvironment.HOME_DIR, jbossHomeDir.getAbsolutePath());

        setupCleanDirectories(jbossHomeDir, systemProps);

        File modulesDir = new File(jbossHomeDir + "/modules");
        final ModuleLoader moduleLoader = InitialModuleLoaderFactory.getModuleLoader(modulesDir, systemPackages);

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

        // Load the server Module and get its ClassLoader
        final ModuleIdentifier serverModuleId = ModuleIdentifier.create("org.jboss.as.server");
        final Module serverModule = moduleLoader.loadModule(serverModuleId);
        final ModuleClassLoader serverModuleClassLoader = serverModule.getClassLoader();

        StandaloneServer standaloneServer = new StandaloneServer() {

            private Object serviceContainer;

            @Override
            public void start() throws ServerStartException {
                try {
                    // Determine the ServerEnvironment
                    Class<?> serverMainClass = serverModuleClassLoader.loadClass(NewMain.class.getName());
                    Method determineEnvironmentMethod = serverMainClass.getMethod("determineEnvironment", String[].class, Properties.class, Map.class);
                    Object serverEnvironment = determineEnvironmentMethod.invoke(null, new String[0], systemProps, systemEnv);

                    Class<?> bootstrapFactoryClass = serverModuleClassLoader.loadClass(NewBootstrap.Factory.class.getName());
                    Method newInstanceMethod = bootstrapFactoryClass.getMethod("newInstance");
                    Object bootstrap = newInstanceMethod.invoke(null);

                    Class<?> configurationClass = serverModuleClassLoader.loadClass(NewBootstrap.Configuration.class.getName());
                    Constructor<?> configurationCtor = configurationClass.getConstructor();
                    Object configuration = configurationCtor.newInstance();

                    Method setServerEnvironmentMethod = configurationClass.getMethod("setServerEnvironment", serverEnvironment.getClass());
                    setServerEnvironmentMethod.invoke(configuration, serverEnvironment);

                    Method setModuleLoaderMethod = configurationClass.getMethod("setModuleLoader", ModuleLoader.class);
                    setModuleLoaderMethod.invoke(configuration, moduleLoader);

                    Class<?> bootstrapClass = serverModuleClassLoader.loadClass(NewBootstrap.class.getName());
                    Method bootstrapStartMethod = bootstrapClass.getMethod("start", configurationClass, List.class);
                    Object future = bootstrapStartMethod.invoke(bootstrap, configuration, Collections.<ServiceActivator>emptyList());

                    Class<?> asyncFutureClass = serverModuleClassLoader.loadClass(AsyncFuture.class.getName());
                    Method getMethod = asyncFutureClass.getMethod("get");
                    serviceContainer = getMethod.invoke(future);

                } catch (RuntimeException rte) {
                    throw rte;
                } catch (Exception ex) {
                    throw new ServerStartException(ex);
                }
            }

            @Override
            public void stop() {
                if (serviceContainer != null) {
                    try {
                        Class<?> serverContainerClass = serverModuleClassLoader.loadClass(ServiceContainer.class.getName());
                        Method shutdownMethod = serverContainerClass.getMethod("shutdown");
                        shutdownMethod.invoke(serviceContainer);

                        Method awaitTerminationMethod = serverContainerClass.getMethod("awaitTermination");
                        awaitTerminationMethod.invoke(serviceContainer);
                    } catch (RuntimeException rte) {
                        throw rte;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        return standaloneServer;
    }

    static void setupCleanDirectories(File jbossHomeDir, Properties props) {
        File tempRoot = getTempRoot(props);
        if (tempRoot == null) {
            return;
        }

        File originalConfigDir = getFileUnderAsRoot(jbossHomeDir, props, ServerEnvironment.SERVER_CONFIG_DIR, "configuration");
        File orginalDataDir = getFileUnderAsRoot(jbossHomeDir, props, ServerEnvironment.SERVER_DATA_DIR, "data");

        File configDir = new File(tempRoot, "config");
        configDir.mkdir();
        File dataDir = new File(tempRoot, "data");
        dataDir.mkdir();

        copyDirectory(originalConfigDir, configDir);
        copyDirectory(orginalDataDir, dataDir);

        props.put(ServerEnvironment.SERVER_CONFIG_DIR, configDir.getAbsolutePath());
        props.put(ServerEnvironment.SERVER_DATA_DIR, dataDir.getAbsolutePath());

    }

    private static File getFileUnderAsRoot(File jbossHomeDir, Properties props, String propName, String relativeLocation) {
        String prop = props.getProperty(propName, null);
        if (prop == null) {
            prop = props.getProperty(ServerEnvironment.SERVER_BASE_DIR, null);
            if (prop == null) {
                File dir = new File(jbossHomeDir, "standalone/" + relativeLocation);
                if (!dir.exists() && !dir.isDirectory()) {
                    throw new IllegalArgumentException("No directory called 'standalone/' " + relativeLocation + " under " + jbossHomeDir.getAbsolutePath());
                }
                return dir;
            } else {
                File server = new File(prop);
                validateDirectory(ServerEnvironment.SERVER_BASE_DIR, server);
                return new File(server, relativeLocation);
            }
        } else {
            File dir = new File(prop);
            validateDirectory(ServerEnvironment.SERVER_CONFIG_DIR, dir);
            return dir;
        }

    }

    private static File getTempRoot(Properties props) {
        String tempRoot = props.getProperty(JBOSS_EMBEDDED_ROOT, null);
        if (tempRoot == null) {
            return null;
        }

        File root = new File(tempRoot);
        if (!root.exists()) {
            //Attempt to try to create the directory, in case something like target/embedded was specified
            root.mkdirs();
        }
        validateDirectory("jboss.test.clean.root", root);
        root = new File(root, "configs");
        root.mkdir();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        root = new File(root, format.format(new Date()));
        root.mkdir();
        return root;
    }

    private static void validateDirectory(String property, File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("-D" + property + "=" + file.getAbsolutePath() + " does not exist");
        }
        if (!file.isDirectory()) {
            throw new IllegalArgumentException("-D" + property + "=" + file.getAbsolutePath() + " is not a directory");
        }
    }

    private static void copyDirectory(File src, File dest) {
        for (String current : src.list()) {
            final File srcFile = new File(src, current);
            final File destFile = new File(dest, current);

            if (srcFile.isDirectory()) {
                destFile.mkdir();
                copyDirectory(srcFile, destFile);
            } else {
                try {
                    final InputStream in = new BufferedInputStream(new FileInputStream(srcFile));
                    final OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));

                    try {
                        int i;
                        while ((i = in.read()) != -1) {
                            out.write(i);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error copying " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath(), e);
                    } finally {
                        StreamUtils.safeClose(in);
                        StreamUtils.safeClose(out);
                    }

                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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

        StandaloneServer server = create(jbossHomeDir, System.getProperties(), System.getenv());
        server.start();
    }
}
