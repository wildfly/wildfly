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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor;
import org.jboss.arquillian.protocol.jmx.JMXTestRunnerMBean;
import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor.ExecutionType;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.as.arquillian.container.AbstractDeployableContainer;
import org.jboss.as.arquillian.container.JBossAsContainerConfiguration;
import org.jboss.as.arquillian.container.MBeanServerConnectionProvider;

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
            long timeout = 5000;
            boolean testRunnerMBeanAvaialble = false;
            MBeanServerConnection mbeanServer = null;
            while (timeout > 0 && testRunnerMBeanAvaialble == false) {
                if (mbeanServer == null) {
                    try {
                        mbeanServer = getMBeanServerConnection();
                    } catch (Exception ex) {
                        // ignore
                    }
                }

                testRunnerMBeanAvaialble = (mbeanServer != null && mbeanServer.isRegistered(JMXTestRunnerMBean.OBJECT_NAME));

                Thread.sleep(100);
                timeout -= 100;
            }
        } catch (Exception e) {
            throw new LifecycleException("Could not start container", e);
        }
    }

    @Override
    public void stop(Context context) throws LifecycleException {
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
