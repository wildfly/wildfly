/**
 *
 */
package org.jboss.as.host.controller;

import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.persistence.ConfigurationFile;
import org.jboss.as.process.DefaultJvmUtils;

/**
 * Encapsulates the runtime environment for a host controller.
 *
 * @author Brian Stansberry
 */
public class HostControllerEnvironment {


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
     * for specifying the base directory for domain content.
     *
     * <p>Defaults to <tt><em>HOME_DIR</em>/domain</tt>.
     */
    public static final String DOMAIN_BASE_DIR = "jboss.domain.base.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the server configuration URL.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/configuration</tt> .
     */
    public static final String DOMAIN_CONFIG_DIR = "jboss.domain.config.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the directory which JBoss will use for
     * persistent data file storage.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/data</tt>.
     */
    public static final String DOMAIN_DATA_DIR = "jboss.domain.data.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the domain deployment URL.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/content</tt> .
     */
    public static final String DOMAIN_DEPLOYMENT_DIR = "jboss.domain.deployment.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the domain log directory for JBoss.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/<em>log</em></tt>.
     */
    public static final String DOMAIN_LOG_DIR = "jboss.domain.log.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the server home directory for JBoss.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/<em>servers</em></tt>.
     */
    public static final String DOMAIN_SERVERS_DIR = "jboss.domain.servers.dir";

    /**
     * Constant that holds the name of the environment property
     * for specifying the directory which JBoss will use for
     * temporary file storage.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/tmp</tt> .
     */
    public static final String DOMAIN_TEMP_DIR = "jboss.domain.temp.dir";

    /**
     * Common alias between domain and standalone mode. Uses jboss.domain.temp.dir on domain mode,
     * and jboss.server.temp.dir on standalone server mode.
     */
    public static final String CONTROLLER_TEMP_DIR = "jboss.controller.temp.dir";

    /**
     * The default system property used to store bind address information from the command-line (-b).
     */
    public static final String JBOSS_BIND_ADDRESS = "jboss.bind.address";

    /**
     * Prefix for the system property used to store qualified bind address information from the command-line (-bxxx).
     */
    public static final String JBOSS_BIND_ADDRESS_PREFIX = JBOSS_BIND_ADDRESS + ".";

    /**
     * The default system property used to store multicast address information from the command-line (-u).
     */
    public static final String JBOSS_DEFAULT_MULTICAST_ADDRESS = "jboss.default.multicast.address";

    /**
     * The default system property used to store the master Host Controller's native management interface address
     * from the command line.
     */
    public static final String JBOSS_DOMAIN_MASTER_ADDRESS = "jboss.domain.master.address";

    /**
     * The default system property used to store the master Host Controller's native of the master port from the command line.
     */
    public static final String JBOSS_DOMAIN_MASTER_PORT = "jboss.domain.master.port";

    private final Map<String, String> hostSystemProperties;
    private final InetAddress processControllerAddress;
    private final Integer processControllerPort;
    private final InetAddress hostControllerAddress;
    private final Integer hostControllerPort;
    private final File homeDir;
    private final File modulesDir;
    private final File domainBaseDir;
    private final File domainConfigurationDir;
    private final ConfigurationFile hostConfigurationFile;
    private final ConfigurationFile domainConfigurationFile;
    private final File domainDeploymentDir;
    private final File domainDataDir;
    private final File domainLogDir;
    private final File domainServersDir;
    private final File domainTempDir;
    private final File defaultJVM;
    private final boolean isRestart;
    private final boolean backupDomainFiles;
    private final boolean useCachedDc;

    private final InputStream stdin;
    private final PrintStream stdout;
    private final PrintStream stderr;

    private final RunningMode initialRunningMode;


    public HostControllerEnvironment(Map<String, String> hostSystemProperties, boolean isRestart, InputStream stdin, PrintStream stdout, PrintStream stderr,
                                     InetAddress processControllerAddress, Integer processControllerPort, InetAddress hostControllerAddress,
                                     Integer hostControllerPort, String defaultJVM, String domainConfig, String hostConfig,
                                     RunningMode initialRunningMode, boolean backupDomainFiles, boolean useCachedDc) {
        if (hostSystemProperties == null) {
            throw MESSAGES.nullVar("hostSystemProperties");
        }
        this.hostSystemProperties = Collections.unmodifiableMap(hostSystemProperties);

        if (stdin == null) {
             throw MESSAGES.nullVar("stdin");
        }
        this.stdin = stdin;

        if (stdout == null) {
             throw MESSAGES.nullVar("stdout");
        }
        this.stdout = stdout;

        if (stderr == null) {
             throw MESSAGES.nullVar("stderr");
        }
        this.stderr = stderr;

        if (processControllerAddress == null) {
            throw MESSAGES.nullVar("processControllerAddress");
        }
        if (processControllerPort == null) {
            throw MESSAGES.nullVar("processControllerPort");
        }
        if (hostControllerAddress == null) {
            throw MESSAGES.nullVar("hostControllerAddress");
        }
        if (hostControllerPort == null) {
            throw MESSAGES.nullVar("hostControllerPort");
        }
        this.processControllerPort = processControllerPort;
        this.processControllerAddress = processControllerAddress;
        this.hostControllerAddress = hostControllerAddress;
        this.hostControllerPort = hostControllerPort;
        this.isRestart = isRestart;



        File home = getFileFromProperty(HOME_DIR);
        if (home == null) {
           home = new File(System.getProperty("user.dir"));
        }
        this.homeDir = home;
        System.setProperty(HOME_DIR, homeDir.getAbsolutePath());

        File tmp = getFileFromProperty(MODULES_DIR);
        if (tmp == null) {
            tmp = new File(this.homeDir, "modules");
        }
        this.modulesDir = tmp;
        System.setProperty(MODULES_DIR, this.modulesDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_BASE_DIR);
        if (tmp == null) {
            tmp = new File(this.homeDir, "domain");
        }
        this.domainBaseDir = tmp;
        System.setProperty(DOMAIN_BASE_DIR, this.domainBaseDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_CONFIG_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "configuration");
        }
        this.domainConfigurationDir = tmp;
        System.setProperty(DOMAIN_CONFIG_DIR, this.domainConfigurationDir.getAbsolutePath());

        hostConfigurationFile = new ConfigurationFile(domainConfigurationDir, "host.xml", hostConfig);
        domainConfigurationFile = new ConfigurationFile(domainConfigurationDir, "domain.xml", domainConfig);

        tmp = getFileFromProperty(DOMAIN_DEPLOYMENT_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "content");
        }
        this.domainDeploymentDir = tmp;
        System.setProperty(DOMAIN_DEPLOYMENT_DIR, this.domainDeploymentDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_DATA_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "data");
        }
        this.domainDataDir = tmp;
        System.setProperty(DOMAIN_DATA_DIR, this.domainDataDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_LOG_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "log");
        }
        this.domainLogDir = tmp;
        System.setProperty(DOMAIN_LOG_DIR, this.domainLogDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_SERVERS_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "servers");
        }
        this.domainServersDir = tmp;
        System.setProperty(DOMAIN_SERVERS_DIR, this.domainServersDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_TEMP_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "tmp");
        }
        this.domainTempDir = tmp;
        System.setProperty(DOMAIN_TEMP_DIR, this.domainTempDir.getAbsolutePath());

        if(defaultJVM != null) {
            if (defaultJVM.equals("java")) {
                defaultJVM = DefaultJvmUtils.findJavaExecutable(DefaultJvmUtils.getCurrentJvmHome());
            }
            this.defaultJVM = new File(defaultJVM);
        } else {
            this.defaultJVM = null;
        }

        this.backupDomainFiles = backupDomainFiles;
        this.useCachedDc = useCachedDc;
        this.initialRunningMode = initialRunningMode;
    }

    /**
     * Gets the original System.in for this process. This should only
     * be used for communication with the process controller that spawned this process.
     *
     * @return stdin
     */
    public InputStream getStdin() {
        return stdin;
    }

    /**
     * Gets the original System.out for this process. This should only
     * be used for communication with the process controller that spawned this process.
     *
     * @return stdout
     */
    public PrintStream getStdout() {
        return stdout;
    }

    /**
     * Gets the original System.err for this process. This should only
     * be used for communication with the process controller that spawned this process.
     *
     * @return stderr
     */
    public PrintStream getStderr() {
        return stderr;
    }

    /**
     * Gets the address the process controller passed to this process
     * to use in communicating with it.
     *
     * @return the process controller's address
     */
    public InetAddress getProcessControllerAddress() {
        return processControllerAddress;
    }

    /**
     * Gets the port number the process controller passed to this process
     * to use in communicating with it.
     *
     * @return the process controller's port
     */
    public Integer getProcessControllerPort() {
        return processControllerPort;
    }

    /**
     * Gets the address the process controller told us to listen for communication from the servers.
     *
     * @return the host controller's address
     */
    public InetAddress getHostControllerAddress() {
        return hostControllerAddress;
    }

    /**
     * Gets the port the process controller told us to listen for communication from the servers.
     *
     * @return the host controller's port
     */
    public Integer getHostControllerPort() {
        return hostControllerPort;
    }

    /**
     * Gets whether this was a restarted host controller.
     *
     * @return if it was restarted
     */
    public boolean isRestart() {
        return isRestart;
    }

    /**
     * Whether we should grab a copy of the master Domain Controller's files on startup.
     * This only has an effect if we are in slave mode
     *
     * @return <code>true</code> if we should grab the files
     */
    public boolean isBackupDomainFiles() {
        return backupDomainFiles;
    }

    /**
     * Whether we should try to start up with our copy of the domain controller.
     * This only has an effect if we are in slave mode
     *
     * @return <code>true</code> if we should grab the files
     */
    public boolean isUseCachedDc() {
        return useCachedDc;
    }

    public RunningMode getInitialRunningMode() {
        return initialRunningMode;
    }

    public File getHomeDir() {
        return homeDir;
    }

    public File getModulesDir() {
        return modulesDir;
    }

    public File getDomainBaseDir() {
        return domainBaseDir;
    }

    public File getDomainConfigurationDir() {
        return domainConfigurationDir;
    }

    public File getDomainDeploymentDir() {
        return domainDeploymentDir;
    }

    public File getDomainDataDir() {
        return domainDataDir;
    }

    public File getDomainLogDir() {
        return domainLogDir;
    }

    public File getDomainServersDir() {
        return domainServersDir;
    }

    public File getDomainTempDir() {
        return domainTempDir;
    }

    public File getDefaultJVM() {
        return defaultJVM;
    }

    public ConfigurationFile getHostConfigurationFile() {
        return hostConfigurationFile;
    }

    public ConfigurationFile getDomainConfigurationFile() {
        return domainConfigurationFile;
    }

    public Map<String, String> getHostSystemProperties() {
        return hostSystemProperties;
    }


    /**
     * Get a File from configuration.
     * @return the CanonicalFile form for the given name.
     */
    private File getFileFromProperty(final String name) {
       String value = hostSystemProperties.get(name);
       if (value != null) {
          File f = new File(value);
          return f;
       }

       return null;
    }
}
