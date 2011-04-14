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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor;
import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor.ExecutionType;
import org.jboss.arquillian.protocol.jmx.JMXTestRunnerMBean;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.as.arquillian.container.AbstractDeployableContainer;
import org.jboss.as.arquillian.container.JBossAsContainerConfiguration;
import org.jboss.as.arquillian.container.MBeanServerConnectionProvider;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.ServerController;
import org.jboss.dmr.ModelNode;

import javax.management.MBeanServerConnection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * JBossASEmbeddedContainer
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public class JBossAsManagedContainer extends AbstractDeployableContainer {

    private final Logger log = Logger.getLogger(JBossAsManagedContainer.class.getName());
    private MBeanServerConnectionProvider provider;
    private Process process;
    private Thread shutdownThread;

    @Override
    public void setup(Context context, Configuration configuration) {
        super.setup(context, configuration);
        JBossAsContainerConfiguration config = getContainerConfiguration();
        provider = new MBeanServerConnectionProvider(config.getBindAddress(), config.getJmxPort());
    }

    @Override
    public void start(Context context) throws LifecycleException {
        try {
            String jbossHomeKey = "jboss.home";
            String jbossHomeDir = System.getProperty(jbossHomeKey);
            if (jbossHomeDir == null)
                throw new IllegalStateException("Cannot find system property: " + jbossHomeKey);

            final String additionalJavaOpts = System.getProperty("jboss.options");

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

            long timeout = getContainerConfiguration().getStartupTimeout();

            // FIXME JBAS-9312 reenable when whatever causes it to intermittently hang is resolved
//            boolean serverAvailable = false;
//            while (timeout > 0 && serverAvailable == false) {
//
//                serverAvailable = isServerStarted();
//
//                Thread.sleep(100);
//                timeout -= 100;
//            }
//
//            if (!serverAvailable) {
//                throw new TimeoutException(String.format("Managed server was not started within [%d] ms", timeout));
//            }

            boolean testRunnerMBeanAvailable = false;
            MBeanServerConnection mbeanServer = null;
            while (timeout > 0 && testRunnerMBeanAvailable == false) {
                if (mbeanServer == null) {
                    try {
                        mbeanServer = getMBeanServerConnection();
                    } catch (Exception ex) {
                        // ignore
                    }
                }

                testRunnerMBeanAvailable = (mbeanServer != null && mbeanServer.isRegistered(JMXTestRunnerMBean.OBJECT_NAME));

                Thread.sleep(100);
                timeout -= 100;
            }

            if (!testRunnerMBeanAvailable) {
                throw new TimeoutException(String.format("Could not connect to the managed server's MBeanServer within [%d] ms", timeout));
            }

        } catch (Exception e) {
            throw new LifecycleException("Could not start container", e);
        }
    }

    @Override
    public void stop(Context context) throws LifecycleException {
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

    @Override
    protected MBeanServerConnection getMBeanServerConnection() {
        return provider.getConnection();
    }

    @Override
    protected ContainerMethodExecutor getContainerMethodExecutor() {
        return new JMXMethodExecutor(getMBeanServerConnection(), ExecutionType.REMOTE);
   }

    private boolean isServerStarted() {
        try {
            ModelNode op = Util.getEmptyOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS.toModelNode());
            op.get(NAME).set("server-state");

            ModelNode rsp = getModelControllerClient().execute(op);
            return SUCCESS.equals(rsp.get(OUTCOME).asString()) && !ServerController.State.STARTING.toString().equals(rsp.get(RESULT).asString());
        }
        catch (Exception ignored) {
            // ignore, as we will get exceptions until the management comm services start
        }
        return false;
    }

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
            final InputStreamReader reader = new InputStreamReader(stream);
            final boolean writeOutput = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

                @Override
                public Boolean run() {
                    // this needs a better name
                    String val = System.getProperty("org.jboss.as.writeconsole");
                    return val != null && "true".equals(val);
                }
            });
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
