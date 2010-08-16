/**
 * 
 */
package org.jboss.as.server.manager;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Properties;

/**
 * Encapsulates the runtime environment for a {@link ServerManager}.
 * 
 * @author Brian Stansberry
 */
public class ServerManagerEnvironment {
    

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
    private final InetAddress processManagerAddress;
    private final Integer processManagerPort;
    private final File homeDir;
    private final File modulesDir;
    private final File domainBaseDir;
    private final File domainConfigurationDir;
    private final File domainDataDir;
    private final File domainLogDir;
    private final File domainServersDir;
    private final File domainTempDir;
    
    
    private final InputStream stdin;
    private final PrintStream stdout;
    private final PrintStream stderr;
    
    public ServerManagerEnvironment(Properties props, InputStream stdin, PrintStream stdout, PrintStream stderr, 
            String processName, InetAddress processManagerAddress, Integer processManagerPort) {
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
        if (processManagerAddress == null) {
            throw new IllegalArgumentException("processManagerAddress is null");
        }
        if (processManagerPort == null) {
            throw new IllegalArgumentException("processManagerPort is null");
        }
        this.processName = processName;
        this.processManagerPort = processManagerPort;
        this.processManagerAddress = processManagerAddress;

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
     * Get the process name of this process, needed to inform the process manager we have started
     * 
     * @return the process name 
     */
    public String getProcessName() {
        return processName;
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

    private static InetAddress findLocalhost() {
        // FIXME implement findLocalhost
        throw new UnsupportedOperationException("implement me");
    }

    /**
     * Get a File from configuration.
     * @return the CanonicalFile form for the given name.
     */
    private File getFileFromProperty(final String name)
    {
       String value = props.getProperty(name, null);
       if (value != null)
       {
          File f = new File(value);
          return f;
       }

       return null;
    }
}
