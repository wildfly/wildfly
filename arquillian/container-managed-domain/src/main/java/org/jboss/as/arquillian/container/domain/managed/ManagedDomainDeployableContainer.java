/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.as.arquillian.container.domain.managed;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.as.arquillian.container.domain.CommonDomainDeployableContainer;
import org.jboss.as.arquillian.container.domain.Domain;
import org.jboss.as.arquillian.container.domain.Domain.Server;
import org.jboss.dmr.ModelNode;
import org.jboss.as.arquillian.container.domain.ManagementClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.ServerMessages;

/**
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ManagedDomainDeployableContainer extends CommonDomainDeployableContainer<ManagedDomainContainerConfiguration> {

    static final String TEMP_CONTAINER_DIRECTORY = "arquillian-temp-container";

    static final String SERVER_BASE_DIR = "domain";
    static final String CONFIG_DIR = "configuration";
    static final String LOG_DIR = "log";
    static final String DATA_DIR = "data";
    static final String SERVERS_DIR = "servers";

    private final Logger log = Logger.getLogger(ManagedDomainDeployableContainer.class.getName());

    private Thread shutdownThread;
    private Process process;

    @Override
    public Class<ManagedDomainContainerConfiguration> getConfigurationClass() {
        return ManagedDomainContainerConfiguration.class;
    }

    @Override
    protected void startInternal() throws LifecycleException {
        ManagedDomainContainerConfiguration config = getContainerConfiguration();

        if (isServerRunning()) {
            if (config.isAllowConnectingToRunningServer()) {
                return;
            } else {
                failDueToRunning();
            }
        }

        try {
            List<String> additionalJavaOptsCmd = createAdditionalJavaOptsCmd(config);
            final String jbossHomeDir = config.getJbossHome();
            String serverBaseDir = getSystemPropertyValue(additionalJavaOptsCmd, "jboss.server.base.dir", jbossHomeDir + File.separatorChar + SERVER_BASE_DIR);

            if (config.isSetupCleanServerBaseDir() || config.getCleanServerBaseDir() != null) {
                final File cleanServerDirectories = setupCleanServerDirectories(serverBaseDir, jbossHomeDir, config.getCleanServerBaseDir());
                replaceSystemPropertyValue(additionalJavaOptsCmd, "jboss.server.base.dir", cleanServerDirectories.toString());
            }

            List<String> loggingOptsCmd = createLoggingOptsCmd(additionalJavaOptsCmd, jbossHomeDir, serverBaseDir);
            List<String> cmd = createCommandLine(config, additionalJavaOptsCmd, loggingOptsCmd);

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
                serverAvailable = getManagementClient().isDomainInRunningState();
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
                throw new TimeoutException(String.format("Managed Domain server was not started within [%d] s",
                        config.getStartupTimeoutInSeconds()));
            }
        } catch (Exception e) {
            throw new LifecycleException("Could not start container", e);
        }
    }

    private List<String> createLoggingOptsCmd(List<String> additionalJavaOptsCmd, String jbossHomeDir, String serverBaseDir) {
        final String bootLogFileDefaultValue = serverBaseDir + File.separatorChar + LOG_DIR + File.separatorChar + "process-controller.log";
        final String loggingConfigurationDefaultValue = serverBaseDir + File.separatorChar + CONFIG_DIR + File.separatorChar + "logging.properties";
        final String bootLogFileValue = getSystemPropertyValue(additionalJavaOptsCmd, "org.jboss.boot.log.file", getFile(bootLogFileDefaultValue, jbossHomeDir).getAbsolutePath());
        final String loggingConfigurationValue = getSystemPropertyValue(additionalJavaOptsCmd, "logging.configuration", getFile(loggingConfigurationDefaultValue, jbossHomeDir).toURI().toString());
        List<String> relativeJavaOptsCmd = new ArrayList<String>();
        relativeJavaOptsCmd.add("-Dorg.jboss.boot.log.file=" + bootLogFileValue);
        relativeJavaOptsCmd.add("-Dlogging.configuration=" + loggingConfigurationValue);
        return relativeJavaOptsCmd;
    }

    private List<String> createAdditionalJavaOptsCmd(ManagedDomainContainerConfiguration config) {
        final String additionalJavaOpts = config.getJavaVmArguments();
        List<String> additionalJavaOptsCmd = new ArrayList<String>();
        if (additionalJavaOpts != null) {
            for (String opt : additionalJavaOpts.split("\\s+")) {
                additionalJavaOptsCmd.add(opt);
            }
        }
        return additionalJavaOptsCmd;
    }

    @Override
    protected void waitForStart(Domain domain, ManagementClient client) throws LifecycleException {
        waitForAutoStartServersToStart(domain, client);
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

                // Fetch the local-host-name attribute (e.g. "master")
                ModelNode op = new ModelNode();
                op.get("operation").set("read-attribute");
                op.get("name").set("local-host-name");
                ModelNode result = getManagementClient().getControllerClient().execute(op, null);
                String hostName = result.get("result").asString();

                // AS7-6620: Create the shutdown operation and run it asynchronously and wait for process to terminate
                op = new ModelNode();
                op.get("operation").set("shutdown");
                ModelNode address = op.get("address");
                address.add("host", hostName);
                getManagementClient().getControllerClient().executeAsync(op, null);

                process.waitFor();
                process = null;

                shutdown.interrupt();
            }
        } catch (Exception e) {
            throw new LifecycleException("Could not stop container", e);
        }
    }

    private List<String> createCommandLine(ManagedDomainContainerConfiguration config, List<String> additionalJavaOptsCmd, List<String> loggingOptsCmd)
            throws Exception {

        final String jbossHomeDir = config.getJbossHome();
        String modulesPath = config.getModulePath();
        if (modulesPath == null || modulesPath.isEmpty()) {
            modulesPath = jbossHomeDir + File.separatorChar + "modules";
        }
        File modulesDir = new File(modulesPath);
        if (modulesDir.isDirectory() == false)
            throw new IllegalStateException("Cannot find: " + modulesDir);

        String bundlesPath = modulesDir.getParent() + File.separator + "bundles";
        File bundlesDir = new File(bundlesPath);

        File modulesJar = new File(jbossHomeDir + File.separatorChar + "jboss-modules.jar");
        if (!modulesJar.exists())
            throw new IllegalStateException("Cannot find: " + modulesJar);

        List<String> cmd = new ArrayList<String>();
        String javaExec = config.getJavaHome() + File.separatorChar + "bin" + File.separatorChar + "java";
        if (config.getJavaHome().contains(" ")) {
            javaExec = "\"" + javaExec + "\"";
        }
        cmd.add(javaExec);
        cmd.addAll(additionalJavaOptsCmd);

        if (config.isEnableAssertions()) {
            cmd.add("-ea");
        }

        cmd.add("-Djboss.home.dir=" + jbossHomeDir);
        cmd.addAll(loggingOptsCmd);
        cmd.add("-Djboss.bundles.dir=" + bundlesDir.getCanonicalPath());
        cmd.add("-Djboss.domain.default.config=" + config.getDomainConfig());
        cmd.add("-Djboss.host.default.config=" + config.getHostConfig());
        cmd.add("-jar");
        cmd.add(modulesJar.getAbsolutePath());
        cmd.add("-mp");
        cmd.add(modulesPath);
        cmd.add("org.jboss.as.process-controller");
        cmd.add("-jboss-home");
        cmd.add(jbossHomeDir);
        cmd.add("-jvm");
        cmd.add(javaExec);
        cmd.add("--");
        cmd.addAll(loggingOptsCmd);
        cmd.add("--");
        cmd.add("-default-jvm");
        cmd.add(javaExec);

        return cmd;
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

    private boolean isServerRunning() {
        Socket socket = null;
        try {
            socket = new Socket(getContainerConfiguration().getManagementAddress(), getContainerConfiguration()
                    .getManagementPort());
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
        throw new LifecycleException("The server is already running! "
                + "Managed containers do not support connecting to running server instances due to the "
                + "possible harmful effect of connecting to the wrong server. Please stop server before running or "
                + "change to another type of container.\n"
                + "To disable this check and allow Arquillian to connect to a running server, "
                + "set allowConnectingToRunningServer to true in the container configuration");
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

    private void waitForAutoStartServersToStart(Domain domain, ManagementClient client) {
        Set<Server> servers = domain.getAutoStartServers();

        long startupTimeout = getContainerConfiguration().getAutoServerStartupTimeoutInSeconds();
        long timeout = startupTimeout * 1000;
        long sleep = 100;

        while (timeout > 0 && servers.size() > 0) {
            Iterator<Server> serverIterator = servers.iterator();
            while (serverIterator.hasNext()) {
                Server server = serverIterator.next();
                if (client.isServerStarted(server)) {
                    serverIterator.remove();
                }
            }
            try {
                Thread.sleep(sleep);
                timeout -= sleep;
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed waiting for servers to start", e);
            }
        }
        if (timeout <= 0) {
            throw new RuntimeException(
                    "Auto started servers did not start within set timeout [autoServerStartupTimeoutInSeconds=" + startupTimeout + "]. " + servers);
        }
    }

    /**
     * Runnable that consumes the output of the process. If nothing consumes the output the AS will hang on some platforms
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
        while (cmdArgumentsIterator.hasNext()) {
            String cmdArgument = cmdArgumentsIterator.next();
            if (cmdArgument.startsWith(argument)) {
                cmdArgumentsIterator.remove();
            }
        }
        cmdArguments.add(argument + newValue);
    }

    /**
     * Get the value of the system property from a list of command line arguments.
     * @param cmdArguments list of command line arguments
     * @param systemPropertyName name of the system property
     * @param defaultValue the default value
     *
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
     * @param filePathname the file pathname
     * @param jbossHome the jboss home directory
     *
     * @return the File form for the file pathname.
     */
    private static File getFile(final String filePathname, final String jbossHome) {
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
        copyOriginalDirectoryToCleanServerBaseDir(SERVERS_DIR, serverBaseDir, jbossHome, cleanServerBaseDir);
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
    private static void copyOriginalDirectoryToCleanServerBaseDir(String originalDirName, String serverBaseDir, String jbossHome, File cleanServerBaseDir)
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
    private static File createTempServerBaseDirectory() throws IOException {
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
     * @param file the file to delete
     */
    private static void deleteRecursively(File file) {
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
