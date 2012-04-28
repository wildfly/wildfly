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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.jboss.as.arquillian.container.domain.ManagementClient;
import org.jboss.as.arquillian.container.domain.Domain.Server;

/**
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ManagedDomainDeployableContainer extends CommonDomainDeployableContainer<ManagedDomainContainerConfiguration> {

    private static final String CONFIG_PATH = "/domain/configuration/";

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

            List<String> cmd = createCommandLine(config);

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
                serverAvailable = getManagementClient().isDomainInRunningState();
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
                process.destroy();
                process.waitFor();
                process = null;
            }
        } catch (Exception e) {
            throw new LifecycleException("Could not stop container", e);
        }
    }

    private List<String> createCommandLine(ManagedDomainContainerConfiguration config) throws Exception {

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
        if (bundlesDir.isDirectory() == false)
            throw new IllegalStateException("Cannot find: " + bundlesDir);

        final String additionalJavaOpts = config.getJavaVmArguments();

        File modulesJar = new File(jbossHomeDir + File.separatorChar + "jboss-modules.jar");
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

        cmd.add("-Djboss.home.dir=" + jbossHomeDir);
        cmd.add("-Dorg.jboss.boot.log.file=" + jbossHomeDir + "/domain/log/process-controller.log");
        cmd.add("-Dlogging.configuration=file:" + jbossHomeDir + CONFIG_PATH + "logging.properties");
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
        cmd.add("-Dorg.jboss.boot.log.file=" + jbossHomeDir + "/domain/log/host-controller.log");
        cmd.add("-Dlogging.configuration=file:" + jbossHomeDir + CONFIG_PATH + "logging.properties");
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
                + "Managed containers does not support connecting to running server instances due to the "
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
        if(timeout <= 0) {
            throw new RuntimeException(
                    "Auto started servers did not start within set timeout [autoServerStartupTimeoutInSeconds=" + startupTimeout + "]. " + servers);
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