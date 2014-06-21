package org.wildfly.core.testrunner;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * encapsulation of a server process
 *
 * @author Stuart Douglas
 */
public class Server {

    static final String CONFIG_DIR = "configuration";
    static final String SERVER_BASE_DIR = "standalone";
    static final String LOG_DIR = "log";
    static final String DATA_DIR = "data";

    static final String JBOSS_HOME = System.getProperty("jboss.home", System.getenv("JBOSS_HOME"));
    static final String MODULE_PATH = System.getProperty("module.path");
    static final String JVM_ARGS = System.getProperty("jvm.args", "-Xmx512m -XX:MaxPermSize=256m");
    static final String JBOSS_ARGS = System.getProperty("jboss.args");
    static final String JAVA_HOME = System.getProperty("java.home", System.getenv("JAVA_HOME"));
    static final String SERVER_CONFIG = System.getProperty("server.config", "standalone.xml");
    static final int MANAGEMENT_PORT = Integer.getInteger("management.port", 9990);
    static final String MANAGEMENT_ADDRESS = System.getProperty("management.address", "localhost");
    static final String MANAGEMENT_PROTOCOL = System.getProperty("management.protocol", "http-remoting");

    private final Logger log = Logger.getLogger(Server.class.getName());
    private Thread shutdownThread;

    private volatile Process process;
    private volatile ManagementClient client;


    private static boolean processHasDied(final Process process) {
        try {
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            // good
            return false;
        }
    }

    protected void start() {
        try {
            File jbossHomeDir = new File(JBOSS_HOME).getCanonicalFile();
            if (!jbossHomeDir.isDirectory()) {
                throw new IllegalStateException("Cannot find: " + jbossHomeDir);
            }

            String modulesPath = MODULE_PATH;
            if (modulesPath == null || modulesPath.isEmpty()) {
                modulesPath = JBOSS_HOME + File.separatorChar + "modules";
            }


            File modulesJar = new File(JBOSS_HOME + File.separatorChar + "jboss-modules.jar");
            if (!modulesJar.exists()) {
                throw new IllegalStateException("Cannot find: " + modulesJar);
            }

            List<String> cmd = new ArrayList<String>();
            String javaExec = JAVA_HOME + File.separatorChar + "bin" + File.separatorChar + "java";
            if (JAVA_HOME.contains(" ")) {
                javaExec = "\"" + javaExec + "\"";
            }
            cmd.add(javaExec);
            if (JVM_ARGS != null) {
                for (String opt : JVM_ARGS.split("\\s+")) {
                    cmd.add(opt);
                }
            }

            //we are testing, of course we want assertions
            cmd.add("-ea");


            String serverBaseDir = getSystemPropertyValue(cmd, "jboss.server.base.dir", JBOSS_HOME + File.separatorChar + SERVER_BASE_DIR);


            final String bootLogFileDefaultValue = serverBaseDir + File.separatorChar + LOG_DIR + File.separatorChar + "server.log";
            final String loggingConfigurationDefaultValue = serverBaseDir + File.separatorChar + CONFIG_DIR + File.separatorChar + "logging.properties";
            cmd.add("-Djboss.home.dir=" + JBOSS_HOME);
            cmd.add("-Dorg.jboss.boot.log.file=" + getSystemPropertyValue(cmd, "org.jboss.boot.log.file", getFile(bootLogFileDefaultValue, JBOSS_HOME).getAbsolutePath()));
            cmd.add("-Dlogging.configuration=" + getSystemPropertyValue(cmd, "logging.configuration", getFile(loggingConfigurationDefaultValue, JBOSS_HOME).toURI().toString()));
            cmd.add("-jar");
            cmd.add(modulesJar.getAbsolutePath());
            cmd.add("-mp");
            cmd.add(modulesPath);
            cmd.add("org.jboss.as.standalone");
            cmd.add("-server-config");
            cmd.add(SERVER_CONFIG);

            if (JBOSS_ARGS != null) {
                for (String opt : JBOSS_ARGS.split("\\s+")) {
                    cmd.add(opt);
                }
            }

            log.info("Starting container with: " + cmd.toString());
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            new Thread(new ConsoleConsumer()).start();
            final Process proc = process;
            shutdownThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (proc != null) {
                        proc.destroy();
                        try {
                            proc.waitFor();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            Runtime.getRuntime().addShutdownHook(shutdownThread);


            ModelControllerClient modelControllerClient = null;
            try {
                modelControllerClient = ModelControllerClient.Factory.create(
                        MANAGEMENT_PROTOCOL,
                        MANAGEMENT_ADDRESS,
                        MANAGEMENT_PORT,
                        Authentication.getCallbackHandler());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            client = new ManagementClient(modelControllerClient, MANAGEMENT_ADDRESS, MANAGEMENT_PORT, MANAGEMENT_PROTOCOL);

            long startupTimeout = 30;
            long timeout = startupTimeout * 1000;
            boolean serverAvailable = false;
            long sleep = 1000;
            while (timeout > 0 && !serverAvailable) {
                long before = System.currentTimeMillis();
                serverAvailable = client.isServerInRunningState();
                timeout -= (System.currentTimeMillis() - before);
                if (!serverAvailable) {
                    if (processHasDied(proc))
                        break;
                    Thread.sleep(sleep);
                    timeout -= sleep;
                    sleep = Math.max(sleep / 2, 100);
                }
            }
            if (!serverAvailable) {
                destroyProcess();
                throw new RuntimeException("Managed server was not started within 30s");
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not start container", e);
        }
    }

    protected void stop() {
        if (shutdownThread != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
            shutdownThread = null;
        }
        try {
            if (process != null) {
                Thread shutdown = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long timeout = System.currentTimeMillis() + 10000;
                        while (process != null && System.currentTimeMillis() < timeout) {
                            try {
                                process.exitValue();
                                process = null;
                            } catch (IllegalThreadStateException e) {

                            }
                        }

                        // The process hasn't shutdown within 60 seconds. Terminate forcibly.
                        if (process != null) {
                            process.destroy();
                        }
                    }
                });
                shutdown.start();

                // AS7-6620: Create the shutdown operation and run it asynchronously and wait for process to terminate
                ModelNode op = new ModelNode();
                op.get("operation").set("shutdown");
                client.getControllerClient().executeAsync(op, null);

                process.waitFor();
                process = null;

                shutdown.interrupt();
            }
        } catch (Exception e) {
            try {
                if (process != null) {
                    process.destroy();
                    process.waitFor();
                }
            } catch (Exception ignore) {
            }
            throw new RuntimeException("Could not stop container", e);
        }
    }

    private int destroyProcess() {
        if (process == null)
            return 0;
        process.destroy();
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ManagementClient getClient() {
        return client;
    }

    /**
     * Runnable that consumes the output of the process. If nothing consumes the output the AS will hang on some platforms
     *
     * @author Stuart Douglas
     */
    private class ConsoleConsumer implements Runnable {

        @Override
        public void run() {
            final InputStream stream = process.getInputStream();

            try {
                byte[] buf = new byte[32];
                int num;
                // Do not try reading a line cos it considers '\r' end of line
                while ((num = stream.read(buf)) != -1) {
                    System.out.write(buf, 0, num);
                }
            } catch (IOException e) {
            }
        }

    }

    /**
     * Get the value of the system property from a list of command line arguments.
     *
     * @param cmdArguments       list of command line arguments
     * @param systemPropertyName name of the system property
     * @param defaultValue       the default value
     * @return The value of the {@code systemPropertyName} if found in the {@code cmdArguments}
     *         or the {@code defaultValue}
     */
    private String getSystemPropertyValue(List<String> cmdArguments, String systemPropertyName, String defaultValue) {
        final String argument = "-D" + systemPropertyName + "=";
        for (String cmdArgument : cmdArguments) {
            if (cmdArgument.startsWith(argument)) {
                return cmdArgument.substring(argument.length());
            }
        }
        return defaultValue;
    }

    /**
     * Get a File from a file pathname.<br/>
     * If the file or directory denoted by {@code pathname} doesn't exist,
     * check if a relative path to the {@code jbossHome} dir exists.
     *
     * @param filePathname the file pathname
     * @param jbossHome    the jboss home directory
     * @return the File form for the file pathname.
     */
    static File getFile(final String filePathname, final String jbossHome) {
        File result = new File(filePathname);
        // AS7-1752 see if a non-existent relative path exists relative to the home dir
        if (!result.exists() && !result.isAbsolute()) {
            File relative = new File(jbossHome, filePathname);
            if (relative.exists()) {
                result = relative;
            }
        }
        return result;
    }

}
