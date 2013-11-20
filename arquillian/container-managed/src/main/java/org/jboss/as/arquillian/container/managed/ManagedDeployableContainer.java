/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.managed;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.as.arquillian.container.CommonDeployableContainer;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.ServerMessages;
import org.jboss.dmr.ModelNode;

/**
 * The managed deployable container.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public final class ManagedDeployableContainer extends CommonDeployableContainer<ManagedContainerConfiguration> {

    static final String TEMP_CONTAINER_DIRECTORY = "arquillian-temp-container";

    static final String CONFIG_DIR = "configuration";
    static final String SERVER_BASE_DIR = "standalone";
    static final String LOG_DIR = "log";
    static final String DATA_DIR = "data";

    private static final int PORT_RANGE_MIN = 1;
    private static final int PORT_RANGE_MAX = 65535;

    private final Logger log = Logger.getLogger(ManagedDeployableContainer.class.getName());
    private Thread shutdownThread;
    private Process process;

    @Override
    public Class<ManagedContainerConfiguration> getConfigurationClass() {
        return ManagedContainerConfiguration.class;
    }

    private static boolean processHasDied(final Process process) {
        try {
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            // good
            return false;
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        ManagedContainerConfiguration config = getContainerConfiguration();

        if (isServerRunning()) {
            if (config.isAllowConnectingToRunningServer()) {
                return;
            } else {
                failDueToRunning();
            }
        }

        try {
            final String jbossHome = config.getJbossHome();
            File jbossHomeDir = new File(jbossHome).getCanonicalFile();
            if (jbossHomeDir.isDirectory() == false)
                throw new IllegalStateException("Cannot find: " + jbossHomeDir);

            String modulesPath = config.getModulePath();
            if (modulesPath == null || modulesPath.isEmpty()) {
                modulesPath = jbossHome + File.separatorChar + "modules";
            }

            String bundlesPath = config.getBundlePath();
            if (bundlesPath == null || bundlesPath.isEmpty()) {
                bundlesPath = jbossHome + File.separatorChar + "bundles";
            }

            final String additionalJavaOpts = config.getJavaVmArguments();

            File modulesJar = new File(jbossHome + File.separatorChar + "jboss-modules.jar");
            if (!modulesJar.exists())
                throw new IllegalStateException("Cannot find: " + modulesJar);

            List<String> cmd = new ArrayList<String>();
            String javaExec = config.getJavaHome() + File.separatorChar + "bin" + File.separatorChar + "java";
            if (config.getJavaHome().contains(" ")) {
                javaExec = "\"" + javaExec + "\"";
            }
            cmd.add(javaExec);
            if (additionalJavaOpts != null) {
                for (String opt : additionalJavaOpts.split("\\s+")) {
                    cmd.add(opt);
                }
            }

            if (config.isEnableAssertions()) {
                cmd.add("-ea");
            }

            String serverBaseDir = getSystemPropertyValue(cmd, "jboss.server.base.dir", jbossHome + File.separatorChar + SERVER_BASE_DIR);

            // Create a clean server base to run the container; ARQ-638
            if (config.isSetupCleanServerBaseDir() || config.getCleanServerBaseDir() != null) {
                serverBaseDir = setupCleanServerDirectories(serverBaseDir, jbossHome, config.getCleanServerBaseDir()).getAbsolutePath();
                replaceSystemPropertyValue(cmd, "jboss.server.base.dir", serverBaseDir);
            }

            final String bootLogFileDefaultValue = serverBaseDir + File.separatorChar + LOG_DIR + File.separatorChar + "server.log";
            final String loggingConfigurationDefaultValue = serverBaseDir + File.separatorChar + CONFIG_DIR + File.separatorChar + "logging.properties";
            cmd.add("-Djboss.home.dir=" + jbossHome);
            cmd.add("-Dorg.jboss.boot.log.file=" + getSystemPropertyValue(cmd, "org.jboss.boot.log.file", getFile(bootLogFileDefaultValue, jbossHome).getAbsolutePath()));
            cmd.add("-Dlogging.configuration=" + getSystemPropertyValue(cmd, "logging.configuration", getFile(loggingConfigurationDefaultValue, jbossHome).toURI().toString()));
            cmd.add("-Djboss.bundles.dir=" + bundlesPath);
            cmd.add("-jar");
            cmd.add(modulesJar.getAbsolutePath());
            cmd.add("-mp");
            cmd.add(modulesPath);
            cmd.add("org.jboss.as.standalone");
            cmd.add("-server-config");
            cmd.add(config.getServerConfig());
            if (config.isAdminOnly())
                cmd.add("--admin-only");

            // Wait on ports before launching; AS7-4070
            this.waitOnPorts();


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

            long startupTimeout = getContainerConfiguration().getStartupTimeoutInSeconds();
            long timeout = startupTimeout * 1000;
            boolean serverAvailable = false;
            long sleep = 1000;
            while (timeout > 0 && serverAvailable == false) {
                long before = System.currentTimeMillis();
                serverAvailable = getManagementClient().isServerInRunningState();
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
                throw new TimeoutException(String.format("Managed server was not started within [%d] s", getContainerConfiguration().getStartupTimeoutInSeconds()));
            }

        } catch (Exception e) {
            throw new LifecycleException("Could not start container", e);
        }
    }

    /**
     * If specified in the configuration, waits on the specified ports to become
     * available for the specified time, else throws a {@link PortAcquisitionTimeoutException}
     *
     * @throws PortAcquisitionTimeoutException
     */
    private void waitOnPorts() throws PortAcquisitionTimeoutException {
        // Get the config
        final Integer[] ports = this.getContainerConfiguration().getWaitForPorts();
        final int timeoutInSeconds = this.getContainerConfiguration().getWaitForPortsTimeoutInSeconds();

        // For all ports we'll wait on
        if (ports != null && ports.length > 0) {
            for (int i = 0; i < ports.length; i++) {
                final int port = ports[i];
                final long start = System.currentTimeMillis();
                // If not available
                while (!this.isPortAvailable(port)) {

                    // Get time elapsed
                    final int elapsedSeconds = (int) ((System.currentTimeMillis() - start) / 1000);

                    // See that we haven't timed out
                    if (elapsedSeconds > timeoutInSeconds) {
                        throw new PortAcquisitionTimeoutException(port, timeoutInSeconds);
                    }
                    try {
                        // Wait a bit, then try again.
                        Thread.sleep(500);
                    } catch (final InterruptedException e) {
                        Thread.interrupted();
                    }

                    // Log that we're waiting
                    log.warning("Waiting on port " + port + " to become available for "
                            + (timeoutInSeconds - elapsedSeconds) + "s");
                }
            }
        }
    }

    private boolean isPortAvailable(final int port) {
        // Precondition checks
        if (port < PORT_RANGE_MIN || port > PORT_RANGE_MAX) {
            throw new IllegalArgumentException("Port specified is out of range: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            // Attempt both TCP and UDP
            ss = new ServerSocket(port);
            ds = new DatagramSocket(port);
            // So we don't block from using this port while it's in a TIMEOUT state after we release it
            ss.setReuseAddress(true);
            ds.setReuseAddress(true);
            // Could be acquired
            return true;
        } catch (final IOException e) {
            // Swallow
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (ss != null) {
                try {
                    ss.close();
                } catch (final IOException e) {
                    // Swallow

                }
            }
        }

        // Couldn't be acquired
        return false;
    }


    @Override
    protected void stopInternal() throws LifecycleException {
        if (shutdownThread != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
            shutdownThread = null;
        }
        try {
            if (process != null) {
                Thread shutdown = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(getContainerConfiguration().getStopTimeoutInSeconds() * 1000);
                        } catch (InterruptedException e) {
                            return;
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
                getManagementClient().getControllerClient().executeAsync(op, null);

                process.waitFor();
                process = null;

                shutdown.interrupt();
            }
        } catch (Exception e) {
            try {
                if(process != null) {
                    process.destroy();
                    process.waitFor();
                }
            }catch (Exception ignore) {}
            throw new LifecycleException("Could not stop container", e);
        }
    }

    private boolean isServerRunning() {
        Socket socket = null;
        try {
            socket = new Socket(
                    getContainerConfiguration().getManagementAddress(),
                    getContainerConfiguration().getManagementPort());
        } catch (Exception ignored) { // nothing is running on defined ports
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    throw new RuntimeException("Could not close isServerStarted socket", e);
                }
            }
        }
        return true;
    }

    private void failDueToRunning() throws LifecycleException {
        throw new LifecycleException(
                "The server is already running! " +
                        "Managed containers do not support connecting to running server instances due to the " +
                        "possible harmful effect of connecting to the wrong server. Please stop server before running or " +
                        "change to another type of container.\n" +
                        "To disable this check and allow Arquillian to connect to a running server, " +
                        "set allowConnectingToRunningServer to true in the container configuration"
        );
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

    /**
     * Runnable that consumes the output of the process. If nothing consumes the output the AS will hang on some platforms
     *
     * @author Stuart Douglas
     */
    private class ConsoleConsumer implements Runnable {

        @Override
        public void run() {
            final InputStream stream = process.getInputStream();
            final boolean writeOutput = getContainerConfiguration().isOutputToConsole();

            try {
                byte[] buf = new byte[32];
                int num;
                // Do not try reading a line cos it considers '\r' end of line
                while ((num = stream.read(buf)) != -1) {
                    if (writeOutput)
                        System.out.write(buf, 0, num);
                }
            } catch (IOException e) {
            }
        }

    }

    /**
     * Replace the value of the system property from a list of command line arguments.
     *
     * @param cmdArguments list of command line arguments
     * @param systemPropertyName name of the system property
     * @param newValue the new value
     */
    private void replaceSystemPropertyValue(List<String> cmdArguments, String systemPropertyName, String newValue) {
        final String argument = "-D" + systemPropertyName + "=";
        final Iterator<String> cmdArgumentsIterator = cmdArguments.iterator();
        while(cmdArgumentsIterator.hasNext()) {
            String cmdArgument = cmdArgumentsIterator.next();
            if (cmdArgument.startsWith(argument)) {
                cmdArgumentsIterator.remove();
            }
        }
        cmdArguments.add(argument + newValue);
    }

    /**
     * Get the value of the system property from a list of command line arguments.
     *
     * @param cmdArguments list of command line arguments
     * @param systemPropertyName name of the system property
     * @param defaultValue the default value
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
     * @param jbossHome the jboss home directory
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

    /**
     * Setup clean directories to run the container.
     * @param serverBaseDir the server base directory
     * @param jbossHome the JBoss home
     * @param cleanServerBaseDirPath the clean server base directory
     */
    static File setupCleanServerDirectories(String serverBaseDir, String jbossHome, String cleanServerBaseDirPath) throws IOException {
        final File cleanServerBaseDir;
        if (cleanServerBaseDirPath != null) {
            cleanServerBaseDir = new File(cleanServerBaseDirPath);
        } else {
            cleanServerBaseDir = createTempServerBaseDirectory();
        }
        if (!cleanServerBaseDir.exists()) {
            throw ServerMessages.MESSAGES.serverBaseDirectoryDoesNotExist(cleanServerBaseDir);
        }
        if (!cleanServerBaseDir.isDirectory()) {
            throw ServerMessages.MESSAGES.serverBaseDirectoryIsNotADirectory(cleanServerBaseDir);
        }
        copyOriginalDirectoryToCleanServerBaseDir(CONFIG_DIR, serverBaseDir, jbossHome, cleanServerBaseDir);
        copyOriginalDirectoryToCleanServerBaseDir(DATA_DIR, serverBaseDir, jbossHome, cleanServerBaseDir);
        // For jboss.server.deployment.scanner.default
        File deploymentsDir = new File(cleanServerBaseDir, "deployments");
        deploymentsDir.mkdir();
        return cleanServerBaseDir;
    }

    /**
     * Copy the original directory to the clean server base directory.
     * @param originalDirName Name of the original directory
     * @param cleanServerBaseDir the clean server base directories
     * @param serverBaseDir the server base directory
     * @param jbossHome the jboss home
     * @throws IOException
     */
    static void copyOriginalDirectoryToCleanServerBaseDir(String originalDirName, String serverBaseDir, String jbossHome, File cleanServerBaseDir)
            throws IOException {
        final File originalDir = getFile(serverBaseDir + File.separatorChar + originalDirName, jbossHome);

        File cleanDir = new File(cleanServerBaseDir, originalDirName);
        cleanDir.mkdir();

        if (originalDir.exists()) {
            copyDirectory(originalDir, cleanDir);
        }
    }

    /**
     * Create a temporary directory to setup clean directories to run the container.
     *
     * @throws IOException
     */
    static File createTempServerBaseDirectory() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File tempContainer = new File(tempDir, TEMP_CONTAINER_DIRECTORY);
        // Delete the previous directory if exists...
        if (tempContainer.exists()) {
            deleteRecursively(tempContainer);
        }
        if (!tempContainer.mkdir()) {
            throw new IOException("Could not create temp directory: " + tempContainer.getAbsolutePath());
        }
        return tempContainer;
    }

    /**
     * Copy directory from {@code src} to {@code dest}.
     *
     * @param src Source directory
     * @param dest Destination directory
     */
    private static void copyDirectory(File src, File dest) {
        for (String current : src.list()) {
            final File srcFile = new File(src, current);
            final File destFile = new File(dest, current);

            if (srcFile.isDirectory()) {
                destFile.mkdir();
                copyDirectory(srcFile, destFile);
            } else {
                try {
                    final InputStream in = new BufferedInputStream(new FileInputStream(srcFile));
                    final OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));

                    try {
                        int i;
                        while ((i = in.read()) != -1) {
                            out.write(i);
                        }
                    } catch (IOException e) {
                        throw ServerMessages.MESSAGES.errorCopyingFile(srcFile.getAbsolutePath(), destFile.getAbsolutePath(), e);
                    } finally {
                        StreamUtils.safeClose(in);
                        StreamUtils.safeClose(out);
                    }

                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Delete a file if exists.
     *
     * @param file the file to delete
     */
    static void deleteRecursively(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (String name : file.list()) {
                    deleteRecursively(new File(file, name));
                }
            }
            file.delete();
        }
    }
}
