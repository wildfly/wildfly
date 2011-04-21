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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

import org.jboss.arquillian.spi.client.container.LifecycleException;
import org.jboss.as.arquillian.container.AbstractDeployableContainer;
import org.jboss.as.arquillian.container.JBossAsCommonConfiguration;
import org.jboss.as.arquillian.container.MBeanServerConnectionProvider;

/**
 * JBossASEmbeddedContainer
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public class JBossAsManagedContainer extends AbstractDeployableContainer<JBossAsManagedConfiguration> {
    private final Logger log = Logger.getLogger(JBossAsManagedContainer.class.getName());

    private MBeanServerConnectionProvider provider;

    private Process process;

    private Thread shutdownThread;

    @Override
    public Class<JBossAsManagedConfiguration> getConfigurationClass() {
        return JBossAsManagedConfiguration.class;
    }

    @Override
    public void setup(JBossAsManagedConfiguration configuration) {
        super.setup(configuration);
        JBossAsCommonConfiguration config = getContainerConfiguration();
        provider = new MBeanServerConnectionProvider(config.getBindAddress(), config.getJmxPort());
    }

    @Override
    public void start() throws LifecycleException {
        try {
            JBossAsManagedConfiguration config = getContainerConfiguration();
            final String jbossHomeDir = config.getJbossHome();
            final String additionalJavaOpts = config.getJavaVmArguments();

            File modulesJar = new File(jbossHomeDir + "/jboss-modules.jar");
            if (modulesJar.exists() == false)
                throw new IllegalStateException("Cannot find: " + modulesJar);

            List<String> cmd = new ArrayList<String>();
            if (config.getJavaHome() != null) {
                cmd.add(config.getJavaHome() + "/bin/java");
            } else {
                cmd.add("java");
            }
            if (additionalJavaOpts != null) {
                for (String opt : additionalJavaOpts.split("\\s+")) {
                    cmd.add(opt);
                }
            }
            cmd.add("-Djboss.home.dir=" + jbossHomeDir);
            cmd.add("-Dorg.jboss.boot.log.file=" + jbossHomeDir + "/standalone/log/boot.log");
            cmd.add("-Dlogging.configuration=file:" + jbossHomeDir + "/standalone/configuration/logging.properties");
            cmd.add("-jar");
            cmd.add(modulesJar.getAbsolutePath());
            cmd.add("-mp");
            cmd.add(jbossHomeDir + "/modules");
            cmd.add("-logmodule");
            cmd.add("org.jboss.logmanager");
            cmd.add("-jaxpmodule");
            cmd.add("javax.xml.jaxp-provider");
            cmd.add("org.jboss.as.standalone");

            log.info("Starting container with: " + cmd.toString());
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            new Thread(new ConsoleConsumer()).start();
            long timeout = config.getStartupTimeoutInSeconds() * 1000;
            boolean testRunnerMBeanAvaialble = false;
            MBeanServerConnection mbeanServer = null;
            do {
                Thread.sleep(100);
                timeout -= 100;
                if (mbeanServer == null) {
                    try {
                        mbeanServer = getMBeanServerConnection();
                    } catch (Exception ex) {
                        // ignore
                    }
                }

                testRunnerMBeanAvaialble = (mbeanServer != null && mbeanServer.isRegistered(OBJECT_NAME));
            } while (timeout > 0 && testRunnerMBeanAvaialble == false);

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

            if (!testRunnerMBeanAvaialble) {
                throw new RuntimeException("Process did not start in time, waited for " + config.getStartupTimeoutInSeconds()
                        + " ms. " + OBJECT_NAME + " was never registered");
            }
        } catch (Exception e) {
            throw new LifecycleException("Could not start container", e);
        }
    }

    @Override
    public void stop() throws LifecycleException {
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

    protected MBeanServerConnection getMBeanServerConnection() {
        return provider.getConnection();
    }

    // protected ProtocolMetaData getProtocolMetaData(String deploymentName) {
    // return new ProtocolMetaData();
    // }

    /**
     * Runnable that consumes the output of the process. If nothing consumes the output the AS will hang on some platforms
     *
     * @author Stuart Douglas
     *
     */
    private class ConsoleConsumer implements Runnable {
        @Override
        public void run() {
            final InputStream stream = process.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            final boolean writeOutput = getContainerConfiguration().isOutputToConsole();
            try {
                String line;
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
