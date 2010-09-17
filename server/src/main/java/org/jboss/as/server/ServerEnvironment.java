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

/**
 *
 */
package org.jboss.as.server;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Properties;

/**
 * Encapsulates the runtime environment for a {@link Server}.
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
     * <p>Defaults to <tt><em>HOME_DIR</em>/server</tt>.
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
     * <p>Defaults to <tt><em>SERVER_BASE_DIR</em>/deployments</tt>.
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

    private final Properties props;
    private final String processName;
    private final InetAddress processManagerAddress;
    private final Integer processManagerPort;
    private final InetAddress serverManagerAddress;
    private final Integer serverManagerPort;
    private final File homeDir;
    private final File modulesDir;
    private final File serverBaseDir;
    private final File serverConfigurationDir;
    private final File serverDataDir;
    private final File serverDeployDir;
    private final File serverLogDir;
    private final File serverTempDir;
    private final boolean standalone;

    private final InputStream stdin;
    private final PrintStream stdout;
    private final PrintStream stderr;

    public ServerEnvironment(Properties props, InputStream stdin, PrintStream stdout, PrintStream stderr,
            String processName, InetAddress processManagerAddress, Integer processManagerPort, InetAddress serverManagerAddress, Integer serverManagerPort, boolean standalone) {
        this.standalone = standalone;
        if (props == null) {
            throw new IllegalArgumentException("props is null");
        }
        this.props = props;

        if (stdin == null) {
             throw new IllegalArgumentException("stdin is null");
        }
        this.stdin = stdin;

        if (stdout == null) {
             throw new IllegalArgumentException("stdout is null");
        }
        this.stdout = stdout;

        if (stderr == null) {
             throw new IllegalArgumentException("stderr is null");
        }
        this.stderr = stderr;

        if (processName == null && !standalone) {
            throw new IllegalArgumentException("processName is null");
        }
        if (processManagerAddress == null && !standalone) {
            throw new IllegalArgumentException("processManagerAddress is null");
        }
        if (processManagerPort == null && !standalone) {
            throw new IllegalArgumentException("processManagerPort is null");
        }
        if (serverManagerAddress == null && !standalone) {
            throw new IllegalArgumentException("serverManagerAddress is null");
        }
        if (serverManagerPort == null && !standalone) {
            throw new IllegalArgumentException("serverManagerPort is null");
        }
        this.processName = processName;
        this.processManagerPort = processManagerPort;
        this.processManagerAddress = processManagerAddress;
        this.serverManagerAddress = serverManagerAddress;
        this.serverManagerPort = serverManagerPort;

        // Must have HOME_DIR
        this.homeDir = getFileFromProperty(HOME_DIR);
        if (homeDir == null)
           throw new IllegalStateException("Missing configuration value for: " + HOME_DIR);
        System.setProperty(HOME_DIR, homeDir.getAbsolutePath());

        File tmp = getFileFromProperty(MODULES_DIR);
        if (tmp == null) {
            tmp = new File(this.homeDir, "modules");
        }
        this.modulesDir = tmp;
        System.setProperty(MODULES_DIR, this.modulesDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_BASE_DIR);
        if (tmp == null) {
            tmp = new File(this.homeDir, "standalone");
        }
        this.serverBaseDir = tmp;
        System.setProperty(SERVER_BASE_DIR, this.serverBaseDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_CONFIG_DIR);
        if (tmp == null) {
            tmp = new File(this.serverBaseDir, "configuration");
        }
        this.serverConfigurationDir = tmp;
        System.setProperty(SERVER_CONFIG_DIR, this.serverConfigurationDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_DATA_DIR);
        if (tmp == null) {
            tmp = new File(this.serverBaseDir, "data");
        }
        this.serverDataDir = tmp;
        System.setProperty(SERVER_DATA_DIR, this.serverDataDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_DEPLOY_DIR);
        if (tmp == null) {
            tmp = new File(this.serverBaseDir, "deployments");
        }
        this.serverDeployDir = tmp;
        System.setProperty(SERVER_DEPLOY_DIR, this.serverDeployDir.getAbsolutePath());


        tmp = getFileFromProperty(SERVER_LOG_DIR);
        if (tmp == null) {
            tmp = new File(this.serverBaseDir, "log");
        }
        this.serverLogDir = tmp;

        System.setProperty(SERVER_LOG_DIR, this.serverLogDir.getAbsolutePath());

        tmp = getFileFromProperty(SERVER_TEMP_DIR);
        if (tmp == null) {
            tmp = new File(this.serverBaseDir, "tmp");
        }
        this.serverTempDir = tmp;
        System.setProperty(SERVER_TEMP_DIR, this.serverTempDir.getAbsolutePath());

    }

    /**
     * Gets the original System.in for this process. This should only
     * be used for communication with the process manager that spawned this process.
     *
     * @return stdin
     */
    public InputStream getStdin() {
        return stdin;
    }

    /**
     * Gets the original System.out for this process. This should only
     * be used for communication with the process manager that spawned this process.
     *
     * @return stdout
     */
    public PrintStream getStdout() {
        return stdout;
    }

    /**
     * Gets the original System.err for this process. This should only
     * be used for communication with the process manager that spawned this process.
     *
     * @return stderr
     */
    public PrintStream getStderr() {
        return stderr;
    }

    /**
     * Gets the address, if any, the process manager passed to this process
     * to use in communicating with it.
     *
     * @return the process manager's address, or <code>null</code> if
     *         none was provided
     */
    public InetAddress getProcessManagerAddress() {
        return processManagerAddress;
    }

    /**
     * Gets the port number, if any, the process manager passed to this process
     * to use in communicating with it.
     *
     * @return the process manager's port, or <code>null</code> if
     *         none was provided
     */
    public Integer getProcessManagerPort() {
        return processManagerPort;
    }

    /**
     * Gets the address, if any, of the server manager that we need to connect to.
     *
     * @return the server manager's address, or <code>null</code> if
     *         none was provided
     */
    public InetAddress getServerManagerAddress() {
        return serverManagerAddress;
    }

    /**
     * Gets the port number, if any, of the server manager that we need to connect to.
     * to use in communicating with it.
     *
     * @return the server manager's port, or <code>null</code> if
     *         none was provided
     */
    public Integer getServerManagerPort() {
        return serverManagerPort;
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

    public File getDomainBaseDir() {
        return serverBaseDir;
    }

    public File getDomainConfigurationDir() {
        return serverConfigurationDir;
    }

    public File getDomainDataDir() {
        return serverDataDir;
    }

    public File getDomainDeployDir() {
        return serverDeployDir;
    }

    public File getDomainLogDir() {
        return serverLogDir;
    }

    public File getDomainTempDir() {
        return serverTempDir;
    }

    public boolean isStandalone() {
        return standalone;
    }

    /**
     * Get a File from configuration.
     * @return the CanonicalFile form for the given name.
     */
    private File getFileFromProperty(final String name) {
       String value = props.getProperty(name, null);
       if (value != null) {
          File f = new File(value);
          return f;
       }

       return null;
    }
}
