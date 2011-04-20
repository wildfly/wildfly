/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.arquillian.container.domain.managed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class DomainStarterUtil {

    private static final ObjectName OBJECT_NAME;
    static {
        try {
            OBJECT_NAME = new ObjectName("jboss.arquillian:service=jmx-test-runner");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Logger log = Logger.getLogger(DomainStarterUtil.class.getName());

    private final long timeout;
    private Process process;
    private Thread shutdownThread;

    private int[] portOffsets;
    private MBeanServerConnectionProvider[] providers;

    public DomainStarterUtil(final long timeout, final InetAddress addr, final int jmxPort, final int[] portOffsets) {
        this.timeout = timeout;
        this.portOffsets = portOffsets;
        providers = new MBeanServerConnectionProvider[portOffsets.length];
        for (int i = 0 ; i < portOffsets.length ; i++) {
            providers[i] = new MBeanServerConnectionProvider(addr, jmxPort + portOffsets[i]);
        }
    }

    public void start() {
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
            cmd.add("-Dorg.jboss.boot.log.file=" + jbossHomeDir + "/domain/log/process-controller/boot.log");
            cmd.add("-Dlogging.configuration=file:" + jbossHomeDir + "/domain/configuration/logging.properties");
            cmd.add("-jar");
            cmd.add(modulesJar.getAbsolutePath());
            cmd.add("-mp");
            cmd.add(jbossHomeDir + "/modules");
            cmd.add("-logmodule");
            cmd.add("org.jboss.logmanager");
            //cmd.add("-jaxpmodule");
            //cmd.add("javax.xml.jaxp-provider");
            cmd.add("org.jboss.as.process-controller");
            cmd.add("-jboss-home");
            cmd.add(jbossHomeDir);
            cmd.add("-jvm");
            cmd.add("java");
            cmd.add("--");
            cmd.add("-Dorg.jboss.boot.log.file=" + jbossHomeDir + "/domain/log/host-controller/boot.log");
            cmd.add("-Dlogging.configuration=file:" + jbossHomeDir + "/domain/configuration/logging.properties");
            if (additionalJavaOpts != null) {
                for (String opt : additionalJavaOpts.split("\\s+")) {
                    cmd.add(opt);
                }
            }
            cmd.add("--");
            cmd.add("-default-jvm");
            cmd.add("java");

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

            long timeout = this.timeout;

            // FIXME JBAS-9312 reenable when whatever causes it to intermittently hang is resolved
            //This will need putting in a loop similar to the mbean check below
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

            final boolean[] testRunnerMBeansAvailable = new boolean[portOffsets.length];
            int available = 0;
            while (timeout > 0 && available < portOffsets.length) {
                for (int i = 0 ; i < portOffsets.length ; i++) {
                    if (!testRunnerMBeansAvailable[i]) {
                        try {
                            MBeanServerConnection mbeanServer = providers[i].getConnection();
                            boolean isAvailable = mbeanServer != null && mbeanServer.isRegistered(OBJECT_NAME);
                            if (isAvailable) {
                                available++;
                                testRunnerMBeansAvailable[i] = true;
                            }
                        } catch (Exception ignore) {
                        }
                    }
                }

                if (available < portOffsets.length) {
                    final long sleep = 100;
                    Thread.sleep(sleep);
                    timeout -= sleep;
                }
            }

            if (available < portOffsets.length) {
                ArrayList<Integer> notStartedPorts = new ArrayList<Integer>();
                for (int i = 0 ; i < testRunnerMBeansAvailable.length ; i++) {
                    if (!testRunnerMBeansAvailable[i]) {
                        notStartedPorts.add(portOffsets[i]);
                    }
                }
                throw new TimeoutException(String.format("Could not connect to the managed server's MBeanServer for servers with port offsets %s within [%d] ms", notStartedPorts.toString(), this.timeout));
            }

            log.info("All containers started");
        } catch (Exception e) {
            throw new RuntimeException("Could not start container", e);
        }

    }

    public void stop() {
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
            throw new RuntimeException("Could not stop container", e);
        }
    }

    public static void main(String[] args) throws Exception {
        DomainStarterUtil starterUtil = new DomainStarterUtil(20000, InetAddress.getByName("127.0.0.1"), 1090, new int[]{0,150}) ;
        starterUtil.start();
        System.out.println("--------- STARTED");
        starterUtil.stop();
        System.out.println("--------- STOPPED");
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

    public final class MBeanServerConnectionProvider {
        private final InetAddress hostAddr;
        private final int port;

        private JMXConnector jmxConnector;

        public MBeanServerConnectionProvider(InetAddress hostAddr, int port) {
            this.hostAddr = hostAddr;
            this.port = port;
        }

        public MBeanServerConnection getConnection() {
            String host = hostAddr.getHostAddress();
            String urlString = System.getProperty("jmx.service.url", "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
            try {
                if (jmxConnector == null) {
                    log.fine("Connecting JMXConnector to: " + urlString);
                    JMXServiceURL serviceURL = new JMXServiceURL(urlString);
                    jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
                }
                return jmxConnector.getMBeanServerConnection();
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot obtain MBeanServerConnection to: " + urlString, ex);
            }
        }
    }
}
