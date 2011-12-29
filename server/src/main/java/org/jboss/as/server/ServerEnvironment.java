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

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.persistence.ConfigurationFile;
import org.jboss.as.version.ProductConfig;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Encapsulates the runtime environment for a server.
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 */
public class ServerEnvironment implements Serializable {

    private static final long serialVersionUID = -349976376447122910L;

    public static enum LaunchType {
        DOMAIN,
        STANDALONE,
        EMBEDDED,
        APPCLIENT,
    }

    // Provide logging
    private static final transient Logger log = Logger.getLogger(ServerEnvironment.class);

    // ///////////////////////////////////////////////////////////////////////
    // Configuration Value Identifiers //
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Constant that holds the name of the system property for specifying the JDK extension directory paths.
     */
    public static final String JAVA_EXT_DIRS = "java.ext.dirs";

    /**
     * Constant that holds the name of the environment property for specifying the home directory for JBoss.
     */
    public static final String HOME_DIR = "jboss.home.dir";

    /**
     * Constant that holds the name of the environment property for specifying the directory from which JBoss will read modules.
     *
     * <p>
     * Defaults to <tt><em>HOME_DIR</em>/modules</tt>/
     */
    public static final String MODULES_DIR = "jboss.modules.dir";

    /**
     * VFS module identifier
     */
    public static final String VFS_MODULE_IDENTIFIER = "org.jboss.vfs";

    /**
     * Constant that holds the name of the environment property for specifying the base directory for server content.
     *
     * <p>
     * Defaults to <tt><em>HOME_DIR</em>/standalone</tt>.
     */
    public static final String SERVER_BASE_DIR = "jboss.server.base.dir";

    /**
     * Constant that holds the name of the environment property for specifying the server configuration URL.
     *
     * <p>
     * Defaults to <tt><em>SERVER_BASE_DIR</em>/configuration</tt> .
     */
    public static final String SERVER_CONFIG_DIR = "jboss.server.config.dir";

    /**
     * Constant that holds the name of the environment property for specifying the directory which JBoss will use for persistent
     * data file storage.
     *
     * <p>
     * Defaults to <tt><em>SERVER_BASE_DIR</em>/data</tt>.
     */
    public static final String SERVER_DATA_DIR = "jboss.server.data.dir";

    /**
     * Constant that holds the name of the environment property for specifying the directory which JBoss will use for
     * deployments.
     *
     * <p>
     * Defaults to <tt><em>SERVER_DATA_DIR</em>/content</tt>.
     */
    public static final String SERVER_DEPLOY_DIR = "jboss.server.deploy.dir";

    /**
     * Constant that holds the name of the environment property for specifying the server log directory for JBoss.
     *
     * <p>
     * Defaults to <tt><em>SERVER_BASE_DIR</em>/<em>log</em></tt>.
     */
    public static final String SERVER_LOG_DIR = "jboss.server.log.dir";

    /**
     * Constant that holds the name of the environment property for specifying the directory which JBoss will use for temporary
     * file storage.
     *
     * <p>
     * Defaults to <tt><em>SERVER_BASE_DIR</em>/tmp</tt> .
     */
    public static final String SERVER_TEMP_DIR = "jboss.server.temp.dir";

    /**
     * Constant that holds the name of the environment property for specifying the directory which JBoss will use for internal
     * system deployments.
     *
     * <p>
     * Defaults to <tt><em>SERVER_DATA_DIR</em>/system-content</tt>.
     */
    public static final String SERVER_SYSTEM_DEPLOY_DIR = "jboss.server.system.deploy.dir";

    /**
     * Common alias between domain and standalone mode. Uses jboss.domain.temp.dir on domain mode,
     * and jboss.server.temp.dir on standalone server mode.
     */
    public static final String CONTROLLER_TEMP_DIR = "jboss.controller.temp.dir";

    /**
     * Constant that holds the name of the system property for specifying the node name within a cluster.
     */
    public static final String NODE_NAME = "jboss.node.name";

    /**
     * Constant that holds the name of the system property for specifying the name of this server instance.
     */
    public static final String SERVER_NAME = "jboss.server.name";

    /**
     * Constant that holds the name of the system property for specifying the local part of the name of the host that this
     * server is running on.
     */
    public static final String HOST_NAME = "jboss.host.name";

    /**
     * Constant that holds the name of the system property for specifying the fully-qualified name of the host that this server
     * is running on.
     */
    public static final String QUALIFIED_HOST_NAME = "jboss.qualified.host.name";

    /**
     * Constant that holds the name of the system property for specifying the max threads used by the bootstrap ServiceContainer.
     */
    public static final String BOOTSTRAP_MAX_THREADS = "org.jboss.server.bootstrap.maxThreads";

    /**
     * The default system property used to store bind address information from the command-line (-b).
     */
    public static final String JBOSS_BIND_ADDRESS = "jboss.bind.address";

    /**
     * Prefix for the system property used to store qualified bind address information from the command-line (-bxxx).
     */
    public static final String JBOSS_BIND_ADDRESS_PREFIX = JBOSS_BIND_ADDRESS + ".";

    /**
     * The default system property used to store bind address information from the command-line (-b).
     */
    public static final String JBOSS_DEFAULT_MULTICAST_ADDRESS = "jboss.default.multicast.address";

    private final LaunchType launchType;
    private final String qualifiedHostName;
    private final String hostName;
    private final String hostControllerName;
    private final String serverName;
    private final String nodeName;

    private final File[] javaExtDirs;

    private final File homeDir;
    private final File modulesDir;
    private final File serverBaseDir;
    private final File serverConfigurationDir;
    private final ConfigurationFile serverConfigurationFile;
    private final File serverDataDir;
    private final File serverDeployDir;
    private final File serverLogDir;
    private final File serverTempDir;
    private final File controllerTempDir;

    private final boolean standalone;
    private final boolean allowModelControllerExecutor;
    private final RunningMode initialRunningMode;
    private final ProductConfig productConfig;

    public ServerEnvironment(final String hostControllerName, final Properties props, final Map<String, String> env, final String serverConfig,
                             final LaunchType launchType, final RunningMode initialRunningMode, ProductConfig productConfig) {
        if (props == null) {
            throw ControllerMessages.MESSAGES.nullVar("props");
        }
        this.launchType = launchType;
        this.standalone = launchType != LaunchType.DOMAIN;

        this.initialRunningMode = initialRunningMode == null ? RunningMode.NORMAL : initialRunningMode;

        this.hostControllerName = hostControllerName;
        if (standalone && hostControllerName != null) {
            throw ServerMessages.MESSAGES.hostControllerNameNonNullInStandalone();
        }
        if (!standalone && hostControllerName == null) {
            throw ServerMessages.MESSAGES.hostControllerNameNullInDomain();
        }

        // Calculate host and default server name
        String hostName = props.getProperty(HOST_NAME);
        String qualifiedHostName = props.getProperty(QUALIFIED_HOST_NAME);
        if (qualifiedHostName == null) {
            // if host name is specified, don't pick a qualified host name that isn't related to it
            qualifiedHostName = hostName;
            if (qualifiedHostName == null) {
                // POSIX-like OSes including Mac should have this set
                qualifiedHostName = env.get("HOSTNAME");
            }
            if (qualifiedHostName == null) {
                // Certain versions of Windows
                qualifiedHostName = env.get("COMPUTERNAME");
            }
            if (qualifiedHostName == null) {
                try {
                    qualifiedHostName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    qualifiedHostName = null;
                }
            }
            if (qualifiedHostName != null && qualifiedHostName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$|:")) {
                // IP address is not acceptable
                qualifiedHostName = null;
            }
            if (qualifiedHostName == null) {
                // Give up
                qualifiedHostName = "unknown-host.unknown-domain";
            }
            qualifiedHostName = qualifiedHostName.trim().toLowerCase();
        }
        this.qualifiedHostName = qualifiedHostName;

        if (hostName == null) {
            // Use the host part of the qualified host name
            final int idx = qualifiedHostName.indexOf('.');
            hostName = idx == -1 ? qualifiedHostName : qualifiedHostName.substring(0, idx);
        }
        this.hostName = hostName;

        // Set up the server name for management purposes
        String serverName = props.getProperty(SERVER_NAME);
        if (serverName == null) {
            serverName = hostName;
        }
        this.serverName = serverName;

        // Set up the clustering node name
        String nodeName = props.getProperty(NODE_NAME);
        if (nodeName == null) {
            nodeName = serverName;
        }
        this.nodeName = nodeName;

        // Java system-wide extension dirs
        javaExtDirs = getFilesFromProperty(JAVA_EXT_DIRS, props);

        // Must have HOME_DIR
        homeDir = getFileFromProperty(HOME_DIR, props);
        if (homeDir == null)
            throw new IllegalStateException("Missing configuration value for: " + HOME_DIR);

        File tmp = getFileFromProperty(MODULES_DIR, props);
        if (tmp == null) {
            tmp = new File(homeDir, "modules");
        }
        modulesDir = tmp;

        tmp = getFileFromProperty(SERVER_BASE_DIR, props);
        if (tmp == null) {
            tmp = new File(homeDir, standalone ? "standalone" : "domain/servers/" + serverName);
        }
        serverBaseDir = tmp;

        tmp = getFileFromProperty(SERVER_CONFIG_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "configuration");
        }
        serverConfigurationDir = tmp;

        serverConfigurationFile = standalone ? new ConfigurationFile(serverConfigurationDir, "standalone.xml", serverConfig) : null;

        tmp = getFileFromProperty(SERVER_DATA_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "data");
        }
        serverDataDir = tmp;

        tmp = getFileFromProperty(SERVER_DEPLOY_DIR, props);
        if (tmp == null) {
            tmp = new File(serverDataDir, "content");
        }
        serverDeployDir = tmp;

        tmp = getFileFromProperty(SERVER_LOG_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "log");
        }
        serverLogDir = tmp;

        tmp = getFileFromProperty(SERVER_TEMP_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "tmp");
        }
        serverTempDir = tmp;

        tmp = getFileFromProperty(CONTROLLER_TEMP_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "tmp");
        }
        controllerTempDir = tmp;

        boolean allowExecutor = true;
        String maxThreads = SecurityActions.getSystemProperty(BOOTSTRAP_MAX_THREADS);
        if (maxThreads != null && maxThreads.length() > 0) {
            try {
                Integer.decode(maxThreads);
                // Property was set to a valid value; user wishes to control core service threads
                allowExecutor = false;
            } catch(NumberFormatException ex) {
                log.warnf(ex, "Failed to parse property(%s), value(%s) as an integer", BOOTSTRAP_MAX_THREADS, maxThreads);
            }
        }
        allowModelControllerExecutor = allowExecutor;

        this.productConfig = productConfig;
    }

    void install() {
        SecurityActions.setSystemProperty(QUALIFIED_HOST_NAME, qualifiedHostName);
        SecurityActions.setSystemProperty(HOST_NAME, hostName);
        SecurityActions.setSystemProperty(SERVER_NAME, serverName);
        SecurityActions.setSystemProperty(NODE_NAME, nodeName);
        SecurityActions.setSystemProperty(HOME_DIR, homeDir.getAbsolutePath());
        SecurityActions.setSystemProperty(MODULES_DIR, modulesDir.getAbsolutePath());
        SecurityActions.setSystemProperty(SERVER_BASE_DIR, serverBaseDir.getAbsolutePath());
        SecurityActions.setSystemProperty(SERVER_CONFIG_DIR, serverConfigurationDir.getAbsolutePath());
        SecurityActions.setSystemProperty(SERVER_DATA_DIR, serverDataDir.getAbsolutePath());
        SecurityActions.setSystemProperty(SERVER_DEPLOY_DIR, serverDeployDir.getAbsolutePath());
        SecurityActions.setSystemProperty(SERVER_LOG_DIR, serverLogDir.getAbsolutePath());
        SecurityActions.setSystemProperty(SERVER_TEMP_DIR, serverTempDir.getAbsolutePath());

        // Register the vfs module as URLStreamHandlerFactory
        try {
            ModuleLoader bootLoader = Module.getBootModuleLoader();
            Module vfsModule = bootLoader.loadModule(ModuleIdentifier.create(VFS_MODULE_IDENTIFIER));
            Module.registerURLStreamHandlerFactoryModule(vfsModule);
        } catch (Exception ex) {
            log.errorf(ex, "Cannot add module '%s' as URLStreamHandlerFactory provider", VFS_MODULE_IDENTIFIER);
        }
    }

    /**
     * Get the name of this server's host controller. For domain-mode servers, this is the name given in the domain configuration. For
     * standalone servers, which do not utilize a host controller, the value should be <code>null</code>.
     *
     * @return server's host controller name if the instance is running in domain mode, or <code>null</code> if running in standalone
     *         mode
     */
    public String getHostControllerName() {
        return hostControllerName;
    }

    /**
     * Get the name of this server instance. For domain-mode servers, this is the name given in the domain configuration. For
     * standalone servers, this is the name either provided in the server configuration, or, if not given, the name specified
     * via system property, or auto-detected based on host name.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Get the fully-qualified host name detected at server startup.
     *
     * @return the qualified host name
     */
    public String getQualifiedHostName() {
        return qualifiedHostName;
    }

    /**
     * Get the local host name detected at server startup.
     *
     * @return the local host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Get the node name used for clustering purposes.
     *
     * @return the node name
     */
    public String getNodeName() {
        return nodeName;
    }

    public File[] getJavaExtDirs() {
        return javaExtDirs.clone();
    }

    public File getHomeDir() {
        return homeDir;
    }

    public File getModulesDir() {
        return modulesDir;
    }

    public File getServerBaseDir() {
        return serverBaseDir;
    }

    public File getServerConfigurationDir() {
        return serverConfigurationDir;
    }

    public ConfigurationFile getServerConfigurationFile() {
        return serverConfigurationFile;
    }

    public File getServerDataDir() {
        return serverDataDir;
    }

    public File getServerDeployDir() {
        return serverDeployDir;
    }

    public File getServerLogDir() {
        return serverLogDir;
    }

    public File getServerTempDir() {
        return serverTempDir;
    }

    public File getControllerTempDir() {
        return controllerTempDir;
    }

    public LaunchType getLaunchType() {
        return launchType;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public RunningMode getInitialRunningMode() {
        return initialRunningMode;
    }

    // package protected for now as this is not a stable API
    boolean isAllowModelControllerExecutor() {
        return allowModelControllerExecutor;
    }

    public ProductConfig getProductConfig() {
        return productConfig;
    }

    /**
     * Determine the number of threads to use for the bootstrap service container. This reads
     * the {@link #BOOTSTRAP_MAX_THREADS} system property and if not set, defaults to 2*cpus.
     * @see Runtime#availableProcessors()
     * @return the maximum number of threads to use for the bootstrap service container.
     */
    public static int getBootstrapMaxThreads() {
        // Base the bootstrap thread on proc count if not specified
        int cpuCount = Runtime.getRuntime().availableProcessors();
        int defaultThreads = cpuCount * 2;
        String maxThreads = SecurityActions.getSystemProperty(BOOTSTRAP_MAX_THREADS);
        if (maxThreads != null && maxThreads.length() > 0) {
            try {
                int max = Integer.decode(maxThreads);
                defaultThreads = Math.max(max, 1);
            } catch(NumberFormatException ex) {
                log.warnf(ex, "Failed to parse property(%s), value(%s) as an integer", BOOTSTRAP_MAX_THREADS, maxThreads);
            }
        }
        return defaultThreads;
    }

    /**
     * Get a File from configuration.
     *
     * @return the CanonicalFile form for the given name.
     */
    private File getFileFromProperty(final String name, final Properties props) {
        String value = props.getProperty(name, null);
        if (value != null) {
            File f = new File(value);
            return f;
        }

        return null;
    }

    private static final File[] NO_FILES = new File[0];

    /**
     * Get a File path list from configuration.
     *
     * @return the CanonicalFile form for the given name.
     */
    private File[] getFilesFromProperty(final String name, final Properties props) {
        String sep = props.getProperty("path.separator");
        String value = props.getProperty(name, null);
        if (value != null) {
            final String[] paths = value.split(Pattern.quote(sep));
            final int len = paths.length;
            final File[] files = new File[len];
            for (int i = 0; i < len; i++) {
                files[i] = new File(paths[i]);
            }
            return files;
        }
        return NO_FILES;
    }
}
