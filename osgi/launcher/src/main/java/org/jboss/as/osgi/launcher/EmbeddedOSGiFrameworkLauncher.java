/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.log.JDKModuleLogger;
import org.osgi.framework.Constants;

/**
 * Launch the Application Server using its EmbeddedServerFactory and activate the
 * OSGi subsystem.
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @author <a href="mailto:mmatloka@gmail.com">Michal Matloka</a>
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 */
public class EmbeddedOSGiFrameworkLauncher {
    private static final String MODULE_ID_LOGMANAGER = "org.jboss.logmanager";
    private static final String MODULE_ID_VFS = "org.jboss.vfs";
    private static final String SYSPROP_KEY_CLASS_PATH = "java.class.path";
    private static final String SYSPROP_KEY_MODULE_PATH = "module.path";
    private static final String SYSPROP_KEY_BUNDLE_PATH = "jboss.bundles.dir";
    private static final String SYSPROP_KEY_LOGMANAGER = "java.util.logging.manager";
    private static final String SYSPROP_KEY_JBOSS_HOME = "jboss.home";
    private static final String SYSPROP_KEY_JBOSS_HOME_DIR = "jboss.home.dir";
    private static final String SYSPROP_KEY_JBOSS_MODULES_SYSTEM_PKGS = "jboss.modules.system.pkgs";
    private static final String SYSPROP_VALUE_JBOSS_LOGMANAGER_PKG = "org.jboss.logmanager";
    private static final String SYSPROP_VALUE_JBOSS_LOGMANAGER = SYSPROP_VALUE_JBOSS_LOGMANAGER_PKG + ".LogManager";

    /**
     * Hook to the server; used in start/stop, created by setup
     */
    private StandaloneServerIndirection server;

    public void configure(Map<String, String> configuration) {
        String jbossHomeProp = SecurityActions.getSystemProperty(SYSPROP_KEY_JBOSS_HOME_DIR);
        if (jbossHomeProp == null) {
            jbossHomeProp = SecurityActions.getSystemProperty(SYSPROP_KEY_JBOSS_HOME);

            if (jbossHomeProp != null)
                SecurityActions.setSystemProperty(SYSPROP_KEY_JBOSS_HOME_DIR, jbossHomeProp);
        }

        File jbossHomeDir = new File(jbossHomeProp).getAbsoluteFile();

        ModuleLoader moduleLoader = setupModuleLoader(new File(jbossHomeDir, "modules"), configuration);
        setupBundlePath(new File(jbossHomeDir, "bundles"));
        setupVfsModule(moduleLoader);
        setupLoggingSystem(moduleLoader);
        setupServer(moduleLoader, jbossHomeDir, configuration);
    }

    private ModuleLoader setupModuleLoader(File modulesDir, Map<String, String> configuration) {
        // Temporary code, can be removed as soon as Framework R5 is implemented
        //
        // The OSGi packages have to be shared between the caller and the framework. Note that also some OSGi 4.3/5.0 packages
        // are included here because these are used by the resolver. When a package is picked up from the parent classloader
        // JBoss Modules also expects all subpackages to be picked up from the parent classloader, therefore these need to be
        // as well. This can probably be removed as soon as we support OSGi 4.3/5.0
        // The following fixes up the OSGi packages, if they are specified on any of these properties.
        String pkgs = fixOSGiPackages(configuration, Constants.FRAMEWORK_SYSTEMPACKAGES, false);
        String pkgs1 = fixOSGiPackages(configuration, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, false);
        String pkgs2 = fixOSGiPackages(configuration, Constants.FRAMEWORK_BOOTDELEGATION, false);

        String allPkgs;
        if (pkgs.length() == 0 && pkgs1.length() == 0 && pkgs2.length() == 0) {
            // Should add these packages to at least one of the above properties, add to FRAMEWORK_SYSTEMPACKAGES_EXTRA
            allPkgs = fixOSGiPackages(configuration, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, true);
        } else {
            allPkgs = pkgs + "," + pkgs1 + "," + pkgs2;
        }
        // End Temporary code.

        // Strip any attributes of the package information as only bare packages need to passed into create() below.
        List<String> sysPgsNoAttrs = new ArrayList<String>();
        sysPgsNoAttrs.add(SYSPROP_VALUE_JBOSS_LOGMANAGER_PKG);
        for (String sp : allPkgs.split(",")) {
            int idx = sp.indexOf(';');
            if (idx >= 0)
                sysPgsNoAttrs.add(sp.substring(0, idx));
            else
                sysPgsNoAttrs.add(sp);
        }
        SecurityActions.setSystemProperty(SYSPROP_KEY_JBOSS_MODULES_SYSTEM_PKGS, sysPgsNoAttrs.toString());

        String classPath = SecurityActions.getSystemProperty(SYSPROP_KEY_CLASS_PATH);
        try {
            // Set up sysprop env
            SecurityActions.clearSystemProperty(SYSPROP_KEY_CLASS_PATH);
            SecurityActions.setSystemProperty(SYSPROP_KEY_MODULE_PATH, modulesDir.getAbsolutePath());

            return Module.getBootModuleLoader();
        } finally {
            // Return to previous state for classpath prop
            SecurityActions.setSystemProperty(SYSPROP_KEY_CLASS_PATH, classPath);
        }
    }

    private void setupBundlePath(File bundlesDir) {
        SecurityActions.setSystemProperty(SYSPROP_KEY_BUNDLE_PATH, bundlesDir.getAbsolutePath());
    }

    private void setupVfsModule(ModuleLoader moduleLoader) {
        ModuleIdentifier vfsModuleID = ModuleIdentifier.create(MODULE_ID_VFS);

        try {
            Module vfsModule = moduleLoader.loadModule(vfsModuleID);
            Module.registerURLStreamHandlerFactoryModule(vfsModule);
        } catch (ModuleLoadException mle) {
            throw new RuntimeException(mle);
        }
    }

    private void setupLoggingSystem(ModuleLoader moduleLoader) {
        ModuleIdentifier logModuleId = ModuleIdentifier.create(MODULE_ID_LOGMANAGER);
        Module logModule;
        try {
            logModule = moduleLoader.loadModule(logModuleId);
        } catch (ModuleLoadException mle) {
            throw new RuntimeException(mle);
        }

        ModuleClassLoader logModuleClassLoader = logModule.getClassLoader();
        ClassLoader tccl = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(logModuleClassLoader);
            SecurityActions.setSystemProperty(SYSPROP_KEY_LOGMANAGER, SYSPROP_VALUE_JBOSS_LOGMANAGER);

            final Class<?> actualLogManagerClass = LogManager.getLogManager().getClass();
            if (actualLogManagerClass == LogManager.class) {
                System.err.println("Could not load JBoss LogManager; the LogManager or Logging subsystem has likely been accessed prior to this initialization.");
            } else {
                Module.setModuleLogger(new JDKModuleLogger());
            }
        } finally {
            // Reset TCCL
            SecurityActions.setContextClassLoader(tccl);
        }
    }

    private void setupServer(ModuleLoader moduleLoader, File jbossHome, Map<String, String> configuration) {
        // Set the launcher properties as system properties. Not the best approach, but we cannot
        // use the DMR at this point yet, see AS7-5882.
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        server = new StandaloneServerIndirection(moduleLoader, jbossHome);
    }

    public void getService(int timeout, String ... nameParts) {
        server.getService(timeout, nameParts);
    }

    public final void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }

    public Object activateOSGiFramework() throws Exception {
        server.activateOSGiSubsystem();

        // ServiceName.toArray() does not produce the correct output for msc 1.0.2.GA.
        // This issue is already fixed on 1.0.3
        // return server.getService(10000, Services.FRAMEWORK_CREATE.toArray());
        return server.getService(10000, "jbosgi", "framework", "CREATE");
    }

    private String fixOSGiPackages(Map<String, String> configuration, String property, boolean force) {
        List<String> systemPackages = new ArrayList<String>();

        String packages = configuration.get(property);
        if ((packages != null && packages.contains("org.osgi.")) || force) {
            systemPackages.add("org.osgi.framework");
            systemPackages.add("org.osgi.framework.launch");
            systemPackages.add("org.osgi.framework.namespace");
            systemPackages.add("org.osgi.framework.wiring");
            systemPackages.add("org.osgi.resource");
            systemPackages.add("org.osgi.service.resolver");
            systemPackages.add("org.osgi.util.tracker");
            systemPackages.add("org.osgi.util.xml");

            StringBuilder sb = new StringBuilder();
            if (packages != null) {
                sb.append(packages);
                sb.append(',');
            }

            for (String sp : systemPackages) {
                sb.append(sp);
                sb.append(',');
            }
            configuration.put(property, sb.toString());
            return sb.toString();
        }
        return "";
    }
}
