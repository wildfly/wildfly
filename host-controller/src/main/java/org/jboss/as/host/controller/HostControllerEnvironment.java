/**
 *
 */
package org.jboss.as.host.controller;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Properties;

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
     * for specifying the server configuration URL.
     *
     * <p>Defaults to <tt><em>SERVER_CONFIG_DIR</em>/host.xml</tt> .
     */
    public static final String DOMAIN_CONFIG_HOST = "jboss.domain.config.host";

    /**
     * Constant that holds the name of the environment property
     * for specifying the server configuration URL.
     *
     * <p>Defaults to <tt><em>SERVER_CONFIG_DIR</em>/domain.xml</tt> .
     */
    public static final String DOMAIN_CONFIG_DOMAIN = "jboss.domain.config.domain";

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
     * for specifying the domain system deployment URL.
     *
     * <p>Defaults to <tt><em>DOMAIN_BASE_DIR</em>/system-content</tt> .
     */
    public static final String DOMAIN_SYSTEM_DEPLOYMENT_DIR = "jboss.domain.system.deployment.dir";

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

    private final Properties props;
    private final String processName;
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
    private final File domainSystemDeploymentDir;
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


    public HostControllerEnvironment(Properties props, boolean isRestart, InputStream stdin, PrintStream stdout, PrintStream stderr,
            String processName, InetAddress processControllerAddress, Integer processControllerPort, InetAddress hostControllerAddress,
            Integer hostControllerPort, String defaultJVM, boolean backupDomainFiles, boolean useCachedDc) {
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

        if (processName == null) {
            throw new IllegalArgumentException("processName is null");
        }
        if (processControllerAddress == null) {
            throw new IllegalArgumentException("processControllerAddress is null");
        }
        if (processControllerPort == null) {
            throw new IllegalArgumentException("processControllerPort is null");
        }
        if (hostControllerAddress == null) {
            throw new IllegalArgumentException("hostControllerAddress is null");
        }
        if (hostControllerPort == null) {
            throw new IllegalArgumentException("hostControllerPort is null");
        }
        this.processName = processName;
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

        hostConfigurationFile = new ConfigurationFile(domainConfigurationDir, "host.xml", props.getProperty(DOMAIN_CONFIG_HOST, "host.xml"));
        domainConfigurationFile = new ConfigurationFile(domainConfigurationDir, "domain.xml", props.getProperty(DOMAIN_CONFIG_DOMAIN, "domain.xml"));

        tmp = getFileFromProperty(DOMAIN_DEPLOYMENT_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "content");
        }
        this.domainDeploymentDir = tmp;
        System.setProperty(DOMAIN_DEPLOYMENT_DIR, this.domainDeploymentDir.getAbsolutePath());

        tmp = getFileFromProperty(DOMAIN_SYSTEM_DEPLOYMENT_DIR);
        if (tmp == null) {
            tmp = new File(this.domainBaseDir, "system-content");
        }
        this.domainSystemDeploymentDir = tmp;
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
     * Get the process name of this process, needed to inform the process controller we have started
     *
     * @return the process name
     */
    public String getProcessName() {
        return processName;
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

    public File getDomainSystemDeploymentDir() {
        return domainSystemDeploymentDir;
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
