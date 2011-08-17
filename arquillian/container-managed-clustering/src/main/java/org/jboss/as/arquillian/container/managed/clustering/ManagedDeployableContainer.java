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
package org.jboss.as.arquillian.container.managed.clustering;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.as.arquillian.container.CommonDeployableContainer;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * JBossAsManagedContainer
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public final class ManagedDeployableContainer extends CommonDeployableContainer<ManagedContainerConfiguration> {

    private final Logger log = Logger.getLogger(ManagedDeployableContainer.class.getName());
    private Thread shutdownThread;
    private Process process;

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

    @Override
    public Class<ManagedContainerConfiguration> getConfigurationClass() {
        return ManagedContainerConfiguration.class;
    }

    @Override
    public void setup(ManagedContainerConfiguration config) {
        super.setup(config);
    }

    @Override
    protected void startInternal() throws LifecycleException {
        try {
            ManagedContainerConfiguration config = getContainerConfiguration();
            final String jbossHomeDir = config.getJbossHome();
            final String additionalJavaOpts = System.getProperty("jboss.options");

            final String modulePath;
            if(config.getModulePath() != null && !config.getModulePath().isEmpty()) {
                modulePath = config.getModulePath();
            } else {
                modulePath = jbossHomeDir + "/modules";
            }

            File modulesJar = new File(jbossHomeDir + "/jboss-modules.jar");
            if (modulesJar.exists() == false)
                throw new IllegalStateException("Cannot find: " + modulesJar);

            List<String> cmd = new ArrayList<String>();
            cmd.add("java");
            if (additionalJavaOpts != null) {
                for (String opt : additionalJavaOpts.split("\\s+")) {
                    cmd.add(opt);
                }
            }

            // Can't figure out why -server-config won't work, so for now just
            // copy clustering-standalone.xml contents to standalone.xml
//            File srcFile = new File(jbossHomeDir + "/standalone/configuration/clustering-standalone.xml");
//            File destFile = new File(jbossHomeDir + "/standalone/configuration/standalone.xml");
//            FileChannel srcChannel = new java.io.FileInputStream(srcFile).getChannel();
//            FileChannel destChannel = new java.io.FileOutputStream(destFile).getChannel();
//            srcChannel.transferTo(0, srcFile.length(), destChannel);
//            srcChannel.close();
//            destChannel.close();

            cmd.add("-Djboss.home.dir=" + jbossHomeDir);
            cmd.add("-Dorg.jboss.boot.log.file=" + jbossHomeDir + "/standalone/log/boot.log");
            cmd.add("-Dlogging.configuration=file:" + jbossHomeDir + "/standalone/configuration/logging.properties");
            cmd.add("-Djboss.modules.dir=" + modulePath);
            cmd.add("-jar");
            cmd.add(modulesJar.getAbsolutePath());
            cmd.add("-mp");
            cmd.add(modulePath);
            cmd.add("-logmodule");
            cmd.add("org.jboss.logmanager");
            cmd.add("-jaxpmodule");
            cmd.add("javax.xml.jaxp-provider");
//            cmd.add("-server-config");
//            cmd.add(jbossHomeDir + "/standalone/configuration/clustering-standalone.xml");
            cmd.add("org.jboss.as.standalone");
            cmd.add("-server-config");
            cmd.add(config.getServerConfig());
            cmd.add("-Djava.net.preferIPv4Stack=true");

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
            while (timeout > 0 && serverAvailable == false) {
                serverAvailable = isServerStarted();
                if (!serverAvailable) {
                    Thread.sleep(100);
                    timeout -= 100;
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

    @Override
    protected void stopInternal() throws LifecycleException {
        if(shutdownThread != null) {
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

    private boolean isServerStarted() {
        try {
            ModelNode op = Util.getEmptyOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS.toModelNode());
            op.get(NAME).set("server-state");

            ModelNode rsp = getModelControllerClient().execute(op);
            return SUCCESS.equals(rsp.get(OUTCOME).asString()) && !ControlledProcessState.State.STARTING.toString().equals(rsp.get(RESULT).asString());
        }
        catch (Exception ignored) {
            // ignore, as we will get exceptions until the management comm services start
        }
        return false;
    }

    /**
     * Runnable that consumes the output of the process. If nothing consumes the output the AS will hang on some platforms
     * @author Stuart Douglas
     */
    private class ConsoleConsumer implements Runnable {

        @Override
        public void run() {
            final InputStream stream = process.getInputStream();
            final InputStreamReader reader = new InputStreamReader(stream);
            final boolean writeOutput = getContainerConfiguration().isOutputToConsole();

            final char[] data = new char[100];
            try {
                for (int read = 0; read != -1; read = reader.read(data)) {
                    if (writeOutput) {
                        System.out.print(data);
                    }
                }
            } catch (IOException e) {
            }
        }

    }
}
