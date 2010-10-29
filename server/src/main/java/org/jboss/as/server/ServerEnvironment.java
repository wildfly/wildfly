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
     * <p>Defaults to <tt><em>SERVER_DATA_DIR</em>/deployments</tt>.
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

    private final String processName;

    private final File homeDir;
    private final File modulesDir;
    private final File serverBaseDir;
    private final File serverConfigurationDir;
    private final File serverDataDir;
    private final File serverDeployDir;
    private final File serverLogDir;
    private final File serverTempDir;
    private final boolean standalone;

    public ServerEnvironment(Properties props, String processName, boolean standalone) {
        this.standalone = standalone;
        if (props == null) {
            throw new IllegalArgumentException("props is null");
        }

        if (processName == null && !standalone) {
            throw new IllegalArgumentException("processName is null");
        }
        this.processName = processName;

        // Must have HOME_DIR
        homeDir = getFileFromProperty(HOME_DIR, props);
        if (homeDir == null)
           throw new IllegalStateException("Missing configuration value for: " + HOME_DIR);
        System.setProperty(HOME_DIR, homeDir.getAbsolutePath());

        File tmp = getFileFromProperty(MODULES_DIR, props);
        if (tmp == null) {
            tmp = new File(homeDir, "modules");
        }
        modulesDir = tmp;
        System.setProperty(MODULES_DIR, modulesDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_BASE_DIR, props);
        if (tmp == null) {
            tmp = new File(homeDir, "standalone");
        }
        serverBaseDir = tmp;
        System.setProperty(SERVER_BASE_DIR, serverBaseDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_CONFIG_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "configuration");
        }
        serverConfigurationDir = tmp;
        System.setProperty(SERVER_CONFIG_DIR, serverConfigurationDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_DATA_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "data");
        }
        serverDataDir = tmp;
        System.setProperty(SERVER_DATA_DIR, serverDataDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_DEPLOY_DIR, props);
        if (tmp == null) {
            tmp = new File(serverDataDir, "content");
        }
        serverDeployDir = tmp;
        System.setProperty(SERVER_DEPLOY_DIR, serverDeployDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_LOG_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "log");
        }
        serverLogDir = tmp;
        System.setProperty(SERVER_LOG_DIR, serverLogDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_TEMP_DIR, props);
        if (tmp == null) {
            tmp = new File(serverBaseDir, "tmp");
        }
        serverTempDir = tmp;
        System.setProperty(SERVER_TEMP_DIR, serverTempDir.getAbsolutePath());
    }

    /**
     * Get the process name of this process, needed to inform the process manager we have started
     *
     * @return the process name
     */
    public String getProcessName() {
        return processName;
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
