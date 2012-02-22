/**
 *
 */
package org.jboss.as.host.controller;

import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.File;
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
     * Constant that holds the name of the system property
     * for specifying the {@link #getHomeDir() home directory}.
     */
    public static final String HOME_DIR = "jboss.home.dir";

    /**
     * Constant that holds the name of the system property for specifying the directory returned from
     * {@link #getModulesDir()}.
     *
     * <p>
     * Defaults to <tt><em>HOME_DIR</em>/modules</tt>/
     * </p>
     *
     * <strong>This system property has no real meaning and should not be regarded as providing any sort of useful
     * information.</strong> The "modules" directory is the default location from which JBoss Modules looks to find
     * modules. However, this behavior is in no way controlled by this system property, nor is it guaranteed that
     * modules will be loaded from only one directory, nor is it guaranteed that the "modules" directory will be one
     * of the directories used. Finally, the structure and contents of any directories from which JBoss Modules loads
     * resources is not something available from this class. Users wishing to interact with the modular classloading
     * system should use the APIs provided by JBoss Modules
     *
     *
     * @deprecated  has no useful meaning
     */
    @Deprecated
    public static final String MODULES_DIR = "jboss.modules.dir";

    /**
     * Constant that holds the name of the system property
     * for specifying {@link #getDomainBaseDir()} the domain base directory}.
     *
     * <p>Defaults to <tt><em>HOME_DIR</em>/domain</tt>.
     */
    public static final String DOMAIN_BASE_DIR = "jboss.domain.base.dir";

    /**
     * Constant that holds the name of the system property
     * for specifying {@link #getDomainConfigurationDir()} the domain configuration directory}.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/configuration</tt> .
     */
    public static final String DOMAIN_CONFIG_DIR = "jboss.domain.config.dir";

    /**
     * Constant that holds the name of the system property
     * for specifying {@link #getDomainDataDir()} the domain data directory}.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/data</tt>.
     */
    public static final String DOMAIN_DATA_DIR = "jboss.domain.data.dir";

    /**
     * Constant that holds the name of the system property
     * for specifying {@link #getDomainContentDir()} the domain content repository directory}.
     *
     * <p>Defaults to <tt><em>DOMAIN_DATA_DIR</em>/content</tt>.
     */
    public static final String DOMAIN_CONTENT_DIR = "jboss.domain.content.dir";

    /**
     * Deprecated variant of {@link #DOMAIN_CONTENT_DIR}.
     *
     * @deprecated use {@link #DOMAIN_CONTENT_DIR}
     */
    @Deprecated
    public static final String DOMAIN_DEPLOYMENT_DIR = "jboss.domain.deployment.dir";

    /**
     * Constant that holds the name of the system property
     * for specifying {@link #getDomainLogDir()} the domain log directory}.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/<em>log</em></tt>.
     */
    public static final String DOMAIN_LOG_DIR = "jboss.domain.log.dir";

    /**
     * Constant that holds the name of the system property
     * for specifying {@link #getDomainServersDir()} the managed domain server parent directory}.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/<em>servers</em></tt>.
     */
    public static final String DOMAIN_SERVERS_DIR = "jboss.domain.servers.dir";

    /**
     * Constant that holds the name of the system property
     * for specifying {@link #getDomainTempDir()} the domain temporary file storage directory}.
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

    /**
     * The system property used to store the name of the default domain configuration file. If not set,
     * the default domain configuration file is "domain.xml". The default domain configuration file is only
     * relevant if the user does not use the {@code -c} or {@code --domain-config} command line switches
     * to explicitly set the domain configuration file.
     */
    public static final String JBOSS_DOMAIN_DEFAULT_CONFIG = "jboss.domain.default.config";

    /**
     * The system property used to store the name of the default host configuration file. If not set,
     * the default domain configuration file is "host.xml". The default domain configuration file is only
     * relevant if the user does not use the {@code --host-config} command line switch
     * to explicitly set the host configuration file.
     */
    public static final String JBOSS_HOST_DEFAULT_CONFIG = "jboss.host.default.config";

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
    private final File domainContentDir;
    private final File domainDataDir;
    private final File domainLogDir;
    private final File domainServersDir;
    private final File domainTempDir;
    private final File defaultJVM;
    private final boolean isRestart;
    private final boolean backupDomainFiles;
    private final boolean useCachedDc;

    private final RunningMode initialRunningMode;
    private final ProductConfig productConfig;
    private final String qualifiedHostName;
    private final String hostName;

    private String hostControllerName;

    public HostControllerEnvironment(Map<String, String> hostSystemProperties, boolean isRestart,
                                     InetAddress processControllerAddress, Integer processControllerPort, InetAddress hostControllerAddress,
                                     Integer hostControllerPort, String defaultJVM, String domainConfig, String hostConfig,
                                     RunningMode initialRunningMode, boolean backupDomainFiles, boolean useCachedDc, ProductConfig productConfig) {

        if (hostSystemProperties == null) {
            throw MESSAGES.nullVar("hostSystemProperties");
        }
        this.hostSystemProperties = Collections.unmodifiableMap(hostSystemProperties);

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

        @SuppressWarnings("deprecation")
        File tmp = getFileFromProperty(MODULES_DIR);
        if (tmp == null) {
            tmp = new File(this.homeDir, "modules");
        }
        this.modulesDir = tmp;
        @SuppressWarnings("deprecation")
        String deprecatedModDir = MODULES_DIR;
        SecurityActions.setSystemProperty(deprecatedModDir, this.modulesDir.getAbsolutePath());

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

        String defaultHostConfig = SecurityActions.getSystemProperty(JBOSS_HOST_DEFAULT_CONFIG, "host.xml");
        hostConfigurationFile = new ConfigurationFile(domainConfigurationDir, defaultHostConfig, hostConfig);
        String defaultDomainConfig = SecurityActions.getSystemProperty(JBOSS_DOMAIN_DEFAULT_CONFIG, "domain.xml");
        domainConfigurationFile = new ConfigurationFile(domainConfigurationDir, defaultDomainConfig, domainConfig);

        tmp = getFileFromProperty(DOMAIN_DATA_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "data");
        }
        this.domainDataDir = tmp;
        SecurityActions.setSystemProperty(DOMAIN_DATA_DIR, this.domainDataDir.getAbsolutePath());

        @SuppressWarnings("deprecation")
        String deprecatedDepDir = DOMAIN_DEPLOYMENT_DIR;
        tmp = getFileFromProperty(DOMAIN_CONTENT_DIR);
        if (tmp == null) {
            tmp = getFileFromProperty(deprecatedDepDir);
        }
        if (tmp == null) {
            tmp = new File(this.domainDataDir, "content");
        }
        this.domainContentDir = tmp;
        SecurityActions.setSystemProperty(DOMAIN_CONTENT_DIR, this.domainContentDir.getAbsolutePath());
        SecurityActions.setSystemProperty(deprecatedDepDir, this.domainContentDir.getAbsolutePath());

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
     * Gets the address the process controller told this Host Controller to listen on for communication from the servers.
     *
     * @return the host controller's address
     */
    public InetAddress getHostControllerAddress() {
        return hostControllerAddress;
    }

    /**
     * Gets the port the process controller told this Host Controller to listen on for communication from the servers.
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
     * Whether we should maintain a copy of the domain configuration file even though we are not the
     * master host controller for the domain. This is only relevant if we are not the master host controller.
     *
     * @return <code>true</code> if we should grab the files
     */
    public boolean isBackupDomainFiles() {
        return backupDomainFiles;
    }

    /**
     * Whether we should try to start up with a locally cached copy of the domain configuration file rather than
     * trying to connect to a master host controller. This only has an effect if we are not configured to
     * act as the master host controller for the domain.
     *
     * @return <code>true</code> if we start with a locally cached copy of the domain configuration file
     */
    public boolean isUseCachedDc() {
        return useCachedDc;
    }

    /**
     * Gets the {@link RunningMode} that was in effect when this Host Controller was launched.
     *
     * @return  the initial running mode
     */
    public RunningMode getInitialRunningMode() {
        return initialRunningMode;
    }

    /**
     * Gets the {@link ProductConfig} detected at startup.
     *
     * @return the product config. Will not be {@code null}
     */
    public ProductConfig getProductConfig() {
        return productConfig;
    }

    /**
     * Gets the root directory for this JBoss installation.
     *
     * @return the root directory
     */
    public File getHomeDir() {
        return homeDir;
    }

    /**
     * <strong>A filesystem location that has no real meaning and should not be regarded as providing any sort of useful
     * information.</strong> The "modules" directory is the default location from which JBoss Modules looks to find
     * modules. However, this behavior is in no way controlled by the value returned by this method, nor is it guaranteed that
     * modules will be loaded from only one directory, nor is it guaranteed that the "modules" directory will be one
     * of the directories used. Finally, the structure and contents of any directories from which JBoss Modules loads
     * resources is not something available from this class. Users wishing to interact with the modular classloading
     * system should use the APIs provided by JBoss Modules.
     *
     * @return a file
     *
     * @deprecated has no reliable meaning
     */
    @Deprecated
    public File getModulesDir() {
        return modulesDir;
    }

    /**
     * Gets the base directory in which managed domain files are stored.
     * <p>Defaults to {@link #getHomeDir() JBOSS_HOME}/domain</p>
     *
     * @return the domain base directory.
     */
    public File getDomainBaseDir() {
        return domainBaseDir;
    }

    /**
     * Gets the directory in which managed domain configuration files are stored.
     * <p>Defaults to {@link #getDomainBaseDir()}  domainBaseDir}/configuration</p>
     *
     * @return the domain configuration directory.
     */
    public File getDomainConfigurationDir() {
        return domainConfigurationDir;
    }

    /**
     * Gets the directory in which a Host Controller or Process Controller can store private internal state that
     * should survive a process restart.
     * <p>Defaults to {@link #getDomainBaseDir()}  domainBaseDir}/data</p>
     *
     * @return the internal state persistent storage directory for the Host Controller and Process Controller.
     */
    public File getDomainDataDir() {
        return domainDataDir;
    }

    /**
     * Gets the directory in which a Host Controller will store domain-managed user content (e.g. deployments or
     * rollout plans.)
     *
     * <p>Defaults to {@link #getDomainDataDir()}  domainDataDir}/content</p>
     *
     * @return the domain managed content storage directory
     */
    public File getDomainContentDir() {
        return domainContentDir;
    }

    /**
     * Deprecated previous name for {@link #getDomainContentDir()}.
     * @return the domain managed content storage directory.
     *
     * @deprecated use {@link #getDomainContentDir()}
     */
    @Deprecated
    public File getDomainDeploymentDir() {
        return domainContentDir;
    }

    /**
     * Gets the directory in which a Host Controller or Process Controller can write log files.
     * <p>Defaults to {@link #getDomainBaseDir()}  domainBaseDir}/log</p>
     *
     * @return the log file directory for the Host Controller and Process Controller.
     */
    public File getDomainLogDir() {
        return domainLogDir;
    }

    /**
     * Gets the directory under domain managed servers will write any persistent data. Each server will
     * have its own subdirectory.
     * <p>Defaults to {@link #getDomainBaseDir()}  domainBaseDir}/servers</p>
     *
     * @return the root directory for domain managed servers
     */
    public File getDomainServersDir() {
        return domainServersDir;
    }

    /**
     * Gets the directory in which a Host Controller or Process Controller can store private internal state that
     * does not need to survive a process restart.
     * <p>Defaults to {@link #getDomainBaseDir()}  domainBaseDir}/tmp</p>
     *
     * @return the internal state temporary storage directory for the Host Controller and Process Controller.
     */
    public File getDomainTempDir() {
        return domainTempDir;
    }

    /**
     * Gets the location of the default java executable to use when launch managed domain servers.
     *
     * @return the location of the default java executable
     */
    public File getDefaultJVM() {
        return defaultJVM;
    }

    /**
     * Initial set of system properties provided to this Host Controller at boot via the command line.
     * @return the properties
     */
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
     * as the {@link #getHostControllerName() host controller name}. Defaults to the portion of
     * {@link #getQualifiedHostName() the qualified host name} following the first '.'.
     *
     * @return the local host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Gets the name by which this host controller is known in the domain. Default to the
     * {@link #getQualifiedHostName() qualified host name} if the {@code name} attribute is not
     * specified on the root element of the host configuration file (e.g. host.xml).
     *
     * @return the name of the host controller
     */
    public String getHostControllerName() {
        return hostControllerName;
    }

    @Override
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

    public ConfigurationFile getHostConfigurationFile() {
        return hostConfigurationFile;
    }

    public ConfigurationFile getDomainConfigurationFile() {
        return domainConfigurationFile;
    }

    /**
     * Get a File from configuration.
     * @param name the name of the property
     * @return the CanonicalFile form for the given name.
     */
    private File getFileFromProperty(final String name) {
        String value = hostSystemProperties.get(name);
        File result = (value != null) ? new File(value) : null;
        // AS7-1752 see if a non-existent relative path exists relative to the home dir
        if (result != null && homeDir != null && !result.exists() && !result.isAbsolute()) {
            File relative = new File(homeDir, value);
            if (relative.exists()) {
                result = relative;
            }
        }
        return result;
    }
}
