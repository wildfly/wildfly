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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.as.arquillian.container.CommonDeployableContainer;

/**
 * The managed deployable container.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public final class ManagedDeployableContainer extends CommonDeployableContainer<ManagedContainerConfiguration> {

    private static final String CONFIG_PATH = "/standalone/configuration/";

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

            cmd.add("-Djboss.home.dir=" + jbossHome);
            cmd.add("-Dorg.jboss.boot.log.file=" + jbossHome + "/standalone/log/boot.log");
            cmd.add("-Dlogging.configuration=file:" + jbossHome + CONFIG_PATH + "logging.properties");
            cmd.add("-Djboss.modules.dir=" + modulesPath);
            cmd.add("-Djboss.bundles.dir=" + bundlesPath);
            cmd.add("-jar");
            cmd.add(modulesJar.getAbsolutePath());
            cmd.add("-mp");
            cmd.add(modulesPath);
            cmd.add("-jaxpmodule");
            cmd.add("javax.xml.jaxp-provider");
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
                serverAvailable = getManagementClient().isServerInRunningState();
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
                process.destroy();
                process.waitFor();
                process = null;
            }
        } catch (Exception e) {
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
                        "Managed containers does not support connecting to running server instances due to the " +
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
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            final boolean writeOutput = getContainerConfiguration().isOutputToConsole();

            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    if (writeOutput) {
                        System.out.println(line);
                    }
                }
            } catch (IOException e) {
            }
        }

    }
}
