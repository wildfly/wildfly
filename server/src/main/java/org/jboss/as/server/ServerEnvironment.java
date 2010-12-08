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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

/**
 * Encapsulates the runtime environment for a server.
 *
 * @author Brian Stansberry
 */
public class ServerEnvironment {


    /////////////////////////////////////////////////////////////////////////
    //                   Configuration Value Identifiers                   //
    /////////////////////////////////////////////////////////////////////////

    /**
     * Constant that holds the name of the environment property
     * for specifying the home directory for JBoss.
     */
    public static final String HOME_DIR = "jboss.home.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the directory from which JBoss will read modules.
     *
     * <p>Defaults to <tt><em>HOME_DIR</em>/modules</tt>/
     */
    public static final String MODULES_DIR = "jboss.modules.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the base directory for server content.
     *
     * <p>Defaults to <tt><em>HOME_DIR</em>/standalone</tt>.
     */
    public static final String SERVER_BASE_DIR = "jboss.server.base.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the server configuration URL.
     *
     * <p>Defaults to <tt><em>SERVER_BASE_DIR</em>/configuration</tt> .
     */
    public static final String SERVER_CONFIG_DIR = "jboss.server.config.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the directory which JBoss will use for
     * persistent data file storage.
     *
     * <p>Defaults to <tt><em>SERVER_BASE_DIR</em>/data</tt>.
     */
    public static final String SERVER_DATA_DIR = "jboss.server.data.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the directory which JBoss will use for
     * deployments.
     *
     * <p>Defaults to <tt><em>SERVER_DATA_DIR</em>/content</tt>.
     */
    public static final String SERVER_DEPLOY_DIR = "jboss.server.deploy.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the server log directory for JBoss.
     *
     * <p>Defaults to <tt><em>SERVER_BASE_DIR</em>/<em>log</em></tt>.
     */
    public static final String SERVER_LOG_DIR = "jboss.server.log.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the directory which JBoss will use for
     * temporary file storage.
     *
     * <p>Defaults to <tt><em>SERVER_BASE_DIR</em>/tmp</tt> .
     */
    public static final String SERVER_TEMP_DIR = "jboss.server.temp.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the directory which JBoss will use for internal
     * system deployments.
     *
     * <p>Defaults to <tt><em>SERVER_DATA_DIR</em>/system-content</tt>.
     */
    public static final String SERVER_SYSTEM_DEPLOY_DIR = "jboss.server.system.deploy.dir";

    private final String qualifiedHostName;
    private final String hostName;
    private final String serverName;
    private final String nodeName;

    private final File homeDir;
    private final File modulesDir;
    private final File serverBaseDir;
    private final File serverConfigurationDir;
    private final File serverDataDir;
    private final File serverDeployDir;
    private final File serverLogDir;
    private final File serverTempDir;
    private final boolean standalone;
    private final File serverSystemDeployDir;

    public ServerEnvironment(Properties props, Map<String, String> env, boolean standalone) {
        this.standalone = standalone;
        if (props == null) {
            throw new IllegalArgumentException("props is null");
        }

        // Calculate host and default server name
        String hostName = props.getProperty("jboss.host.name");
        String qualifiedHostName = props.getProperty("jboss.qualified.host.name");
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
            SecurityActions.setSystemProperty("jboss.qualified.host.name", qualifiedHostName);
        }
        this.qualifiedHostName = qualifiedHostName;

        if (hostName == null) {
            // Use the host part of the qualified host name
            final int idx = qualifiedHostName.indexOf('.');
            hostName = idx == -1 ? qualifiedHostName : qualifiedHostName.substring(0, idx);
            SecurityActions.setSystemProperty("jboss.host.name", hostName);
        }
        this.hostName = hostName;

        // Set up the server name for management purposes
        String serverName = props.getProperty("jboss.server.name");
        if (serverName == null) {
            serverName = hostName;
            SecurityActions.setSystemProperty("jboss.server.name", serverName);
        }
        this.serverName = serverName;

        // Set up the clustering node name
        String nodeName = props.getProperty("jboss.node.name");
        if (nodeName == null) {
            nodeName = serverName;
            SecurityActions.setSystemProperty("jboss.node.name", nodeName);
        }
        this.nodeName = nodeName;

        // Must have HOME_DIR
        homeDir = getFileFromProperty(HOME_DIR, props);
        if (homeDir == null)
           throw new IllegalStateException("Missing configuration value for: " + HOME_DIR);
        SecurityActions.setSystemProperty(HOME_DIR, homeDir.getAbsolutePath());

        File tmp = getFileFromProperty(MODULES_DIR, props);
        if (tmp == null) {
            tmp = new File(homeDir, "modules");
        }
        modulesDir = tmp;
        SecurityActions.setSystemProperty(MODULES_DIR, modulesDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_BASE_DIR, props);
        if (tmp == null) {
            tmp = new File(homeDir, "standalone");
        }
        serverBaseDir = tmp;
        SecurityActions.setSystemProperty(SERVER_BASE_DIR, serverBaseDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_CONFIG_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "configuration");
        }
        serverConfigurationDir = tmp;
        SecurityActions.setSystemProperty(SERVER_CONFIG_DIR, serverConfigurationDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_DATA_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "data");
        }
        serverDataDir = tmp;
        SecurityActions.setSystemProperty(SERVER_DATA_DIR, serverDataDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_DEPLOY_DIR, props);
        if (tmp == null) {
            tmp = new File(serverDataDir, "content");
        }
        serverDeployDir = tmp;
        SecurityActions.setSystemProperty(SERVER_DEPLOY_DIR, serverDeployDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_SYSTEM_DEPLOY_DIR, props);
        if (tmp == null) {
            tmp = new File(serverDataDir, "system-content");
        }
        serverSystemDeployDir = tmp;
        SecurityActions.setSystemProperty(SERVER_SYSTEM_DEPLOY_DIR, serverSystemDeployDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_LOG_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "log");
        }
        serverLogDir = tmp;
        SecurityActions.setSystemProperty(SERVER_LOG_DIR, serverLogDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_TEMP_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "tmp");
        }
        serverTempDir = tmp;
        SecurityActions.setSystemProperty(SERVER_TEMP_DIR, serverTempDir.getAbsolutePath());
    }

    /**
     * Get the name of this server instance.  For domain-mode servers, this is the name
     * given in the domain configuration.  For standalone servers, this is the name either
     * provided in the server configuration, or, if not given, the name specified via system
     * property, or auto-detected based on host name.
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

    public File getServerDataDir() {
        return serverDataDir;
    }

    public File getServerDeployDir() {
        return serverDeployDir;
    }

    public File getServerSystemDeployDir() {
        return serverSystemDeployDir;
    }

    public File getServerLogDir() {
        return serverLogDir;
    }

    public File getServerTempDir() {
        return serverTempDir;
    }

    public boolean isStandalone() {
        return standalone;
    }

    /**
     * Get a File from configuration.
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
}
