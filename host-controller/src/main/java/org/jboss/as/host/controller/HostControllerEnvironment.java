/**
 *
 */
package org.jboss.as.host.controller;

import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.operations.common.ProcessEnvironment;
import org.jboss.as.controller.persistence.ConfigurationFile;
import org.jboss.as.process.DefaultJvmUtils;
import org.jboss.as.version.ProductConfig;

/**
 * Encapsulates the runtime environment for a host controller.
 *
 * @author Brian Stansberry
 */
public class HostControllerEnvironment extends ProcessEnvironment {


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
    private final ProductConfig productConfig;
    private final String qualifiedHostName;
    private final String hostName;

    private String hostControllerName;

    public HostControllerEnvironment(Map<String, String> hostSystemProperties, boolean isRestart, InputStream stdin, PrintStream stdout, PrintStream stderr,
                                     InetAddress processControllerAddress, Integer processControllerPort, InetAddress hostControllerAddress,
                                     Integer hostControllerPort, String defaultJVM, String domainConfig, String hostConfig,
                                     RunningMode initialRunningMode, boolean backupDomainFiles, boolean useCachedDc, ProductConfig productConfig) {
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

        // Calculate host and default server name
        String hostName = hostSystemProperties.get(HOST_NAME);
        String qualifiedHostName = hostSystemProperties.get(QUALIFIED_HOST_NAME);
        if (qualifiedHostName == null) {
            Map<String, String> env = null;
            // if host name is specified, don't pick a qualified host name that isn't related to it
            qualifiedHostName = hostName;
            if (qualifiedHostName == null) {
                env = SecurityActions.getSystemEnvironment();
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
        this.hostControllerName = qualifiedHostName;

        if (hostName == null) {
            // Use the host part of the qualified host name
            final int idx = qualifiedHostName.indexOf('.');
            hostName = idx == -1 ? qualifiedHostName : qualifiedHostName.substring(0, idx);
        }
        this.hostName = hostName;


        File home = getFileFromProperty(HOME_DIR);
        if (home == null) {
           home = new File(System.getProperty("user.dir"));
        }
        this.homeDir = home;
        SecurityActions.setSystemProperty(HOME_DIR, homeDir.getAbsolutePath());

        File tmp = getFileFromProperty(MODULES_DIR);
        if (tmp == null) {
            tmp = new File(this.homeDir, "modules");
        }
        this.modulesDir = tmp;
        SecurityActions.setSystemProperty(MODULES_DIR, this.modulesDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_BASE_DIR);
        if (tmp == null) {
            tmp = new File(this.homeDir, "domain");
        }
        this.domainBaseDir = tmp;
        SecurityActions.setSystemProperty(DOMAIN_BASE_DIR, this.domainBaseDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_CONFIG_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "configuration");
        }
        this.domainConfigurationDir = tmp;
        SecurityActions.setSystemProperty(DOMAIN_CONFIG_DIR, this.domainConfigurationDir.getAbsolutePath());

        hostConfigurationFile = new ConfigurationFile(domainConfigurationDir, "host.xml", hostConfig);
        domainConfigurationFile = new ConfigurationFile(domainConfigurationDir, "domain.xml", domainConfig);

        tmp = getFileFromProperty(DOMAIN_DEPLOYMENT_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "content");
        }
        this.domainDeploymentDir = tmp;
        SecurityActions.setSystemProperty(DOMAIN_DEPLOYMENT_DIR, this.domainDeploymentDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_DATA_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "data");
        }
        this.domainDataDir = tmp;
        SecurityActions.setSystemProperty(DOMAIN_DATA_DIR, this.domainDataDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_LOG_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "log");
        }
        this.domainLogDir = tmp;
        SecurityActions.setSystemProperty(DOMAIN_LOG_DIR, this.domainLogDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_SERVERS_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "servers");
        }
        this.domainServersDir = tmp;
        SecurityActions.setSystemProperty(DOMAIN_SERVERS_DIR, this.domainServersDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_TEMP_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "tmp");
        }
        this.domainTempDir = tmp;
        SecurityActions.setSystemProperty(DOMAIN_TEMP_DIR, this.domainTempDir.getAbsolutePath());

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
        this.productConfig = productConfig;
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

    public ProductConfig getProductConfig() {
        return productConfig;
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
     * Get the fully-qualified host name detected at server startup.
     *
     * @return the qualified host name
     */
    public String getQualifiedHostName() {
        return qualifiedHostName;
    }

    /**
     * Get the local host name detected at server startup. Note that this is not the same
     * as the {@link #getHostControllerName() host controller name}
     *
     * @return the local host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Gets the name by which this host controller is known in the domain.
     *
     * @return the name of the host controller
     */
    public String getHostControllerName() {
        return hostControllerName;
    }

    protected String getProcessName() {
        return hostControllerName;
    }


    @Override
    protected void setProcessName(String processName) {
        if (processName != null) {
            this.hostControllerName = processName;
        }
    }

    @Override
    protected boolean isRuntimeSystemPropertyUpdateAllowed(String propertyName, String propertyValue, boolean bootTime) {
        // Currently any system-property in host.xml should not be applied to the HC runtime. This method
        // should not be invoked.
        throw HostControllerMessages.MESSAGES.hostControllerSystemPropertyUpdateNotSupported();
    }

    @Override
    protected void systemPropertyUpdated(String propertyName, String propertyValue) {
        // no-op
    }


    /**
     * Get a File from configuration.
     * @param name the name of the property
     * @return the CanonicalFile form for the given name.
     */
    private File getFileFromProperty(final String name) {
       String value = hostSystemProperties.get(name);
       return (value != null) ? new File(value) : null;
    }
}
