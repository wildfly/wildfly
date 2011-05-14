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

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.client.helpers.domain.ServerIdentity;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.dmr.ModelNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class DomainLifecycleUtil {

    private static final ObjectName OBJECT_NAME;
    static {
        try {
            OBJECT_NAME = new ObjectName("jboss.arquillian:service=jmx-test-runner");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Logger log = Logger.getLogger(DomainLifecycleUtil.class.getName());

    private final long timeout;
    private Process process;
    private Thread shutdownThread;

    private final InetAddress managementAddress;
    private final int managementPort;
    private DomainClient domainClient;
    private Map<ServerIdentity, ServerStatus> serverStatuses = new HashMap<ServerIdentity, ServerStatus>();
    private Map<ServerIdentity, MBeanServerConnectionProvider> jmxConnectionProviders = new HashMap<ServerIdentity, MBeanServerConnectionProvider>();
    private Map<ServerIdentity, MBeanServerConnection> jmxConnections = new HashMap<ServerIdentity, MBeanServerConnection>();
    private MBeanServerConnectionProvider[] providers;
    private String domainConfigFile;
    private String hostConfigFile;

    private static InetAddress getDefaultHostAddress() {
        String address = System.getProperty("jboss.test.domain.management.address", "127.0.0.1");
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public DomainLifecycleUtil() {
        this(20000, getDefaultHostAddress(), 9999);
    }

    public DomainLifecycleUtil(final long timeout) {
        this(timeout, getDefaultHostAddress(), 9999);
    }

    public DomainLifecycleUtil(final long timeout, final InetAddress managementAddress, final int managementPort) {
        this.timeout = timeout;
        this.managementAddress = managementAddress;
        this.managementPort = managementPort;
    }

    public String getDomainConfigFile() {
        return domainConfigFile;
    }

    public void setDomainConfigFile(String domainConfigFile) {
        this.domainConfigFile = domainConfigFile;
    }

    public String getHostConfigFile() {
        return hostConfigFile;
    }

    public void setHostConfigFile(String hostConfigFile) {
        this.hostConfigFile = hostConfigFile;
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
            if (domainConfigFile != null) {
                cmd.add("-domain-config " + domainConfigFile);
            }
            if (hostConfigFile != null) {
                cmd.add("-host-config " + hostConfigFile);
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

            domainClient = DomainClient.Factory.create(managementAddress, managementPort);

            long timeout = this.timeout;

            boolean serversAvailable = false;
            while (timeout > 0 && serversAvailable == false) {

                serversAvailable = areServersStarted();

                if (!serversAvailable) {
                    Thread.sleep(100);
                    timeout -= 100;
                }
            }

            if (!serversAvailable) {
                throw new TimeoutException(String.format("Managed servers were not started within [%d] ms", timeout));
            }

            Map<ServerIdentity, MBeanServerConnection> connections = new HashMap<ServerIdentity, MBeanServerConnection>();
            for (Map.Entry<ServerIdentity, ServerStatus> entry : serverStatuses.entrySet()) {
                switch (entry.getValue()) {
                case STARTED:
                    connections.put(entry.getKey(), null);
                }
            }


            int available = 0;
            int enabledCount = connections.size();
            while (timeout > 0 && available < enabledCount) {
                for (Map.Entry<ServerIdentity, MBeanServerConnection> entry : connections.entrySet()) {
                    if (entry.getValue() == null) {
                        try {
                            MBeanServerConnectionProvider provider = getMBeanServerConnectionProvider(entry.getKey());
                            MBeanServerConnection mbeanServer = provider == null ? null : provider.getConnection();
                            boolean isAvailable = mbeanServer != null && mbeanServer.isRegistered(OBJECT_NAME);
                            if (isAvailable) {
                                connections.put(entry.getKey(), mbeanServer);
                            }
                        } catch (Exception ignore) {
                        }
                    }
                }

                if (available < enabledCount) {
                    final long sleep = 100;
                    Thread.sleep(sleep);
                    timeout -= sleep;
                }
            }

            if (available < enabledCount) {
                ArrayList<ServerIdentity> notStartedServers = new ArrayList<ServerIdentity>();
                for (Map.Entry<ServerIdentity, MBeanServerConnection> entry : connections.entrySet()) {
                    if (entry.getValue() == null) {
                        notStartedServers.add(entry.getKey());
                    }
                }
                throw new TimeoutException(String.format("Could not connect to the managed server's MBeanServer for servers with port offsets %s within [%d] ms", notStartedServers.toString(), this.timeout));
            }

            log.info("All containers started");
        } catch (Exception e) {
            throw new RuntimeException("Could not start container", e);
        }

    }

    private MBeanServerConnectionProvider getMBeanServerConnectionProvider(ServerIdentity server) throws IOException{
        ModelNode op = new ModelNode();
        op.get("operation").set("read-children-names");
        ModelNode opAddr = op.get("address");
        opAddr.add("host", server.getHostName());
        opAddr.add("server", server.getServerName());
        op.get("child-type").set("socket-binding-group");
        ModelNode result = executeForResult(op);
        String groupName = result.asList().get(0).asString();

        op = new ModelNode();
        op.get("operation").set("read-attribute");
        opAddr = op.get("address");
        opAddr.add("host", server.getHostName());
        opAddr.add("server", server.getServerName());
        opAddr.add("socket-binding-group", groupName);
        opAddr.add("socket-binding", "jmx-connector-registry");
        op.get("name").set("bound");
        result = executeForResult(op);

        if (result.asBoolean(false)) {
            op.get("name").set("bound-address");
            String address = executeForResult(op).asString();
            op.get("name").set("bound-port");
            int port = executeForResult(op).asInt();
            return new MBeanServerConnectionProvider(InetAddress.getByName(address), port);
        }
        return null;
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
        try {
            if (domainClient != null) {
                domainClient.close();
            }
        }  catch (Exception e) {
            throw new RuntimeException("Could not stop DomainClient", e);
        }
    }

    private boolean areServersStarted() {
        try {
            Map<ServerIdentity, ServerStatus> statuses = domainClient.getServerStatuses();
            for (ServerStatus status : statuses.values()) {
                switch (status) {
                    case STOPPING:
                    case STOPPED:
                        return false;
                    default:
                        continue;
                }
            }
            serverStatuses.putAll(statuses);
            return true;
        }
        catch (Exception ignored) {
            // ignore, as we will get exceptions until the management comm services start
        }
        return false;
    }

    private ModelNode executeForResult(ModelNode op) {
        return executeForResult(OperationBuilder.Factory.create(op).build());
    }

    ModelNode executeForResult(Operation op) {
        try {
            ModelNode result = domainClient.execute(op);
            if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
                return result.get("result");
            }
            else if (result.hasDefined("failure-description")) {
                throw new RuntimeException(result.get("failure-description").toString());
            }
            else if (result.hasDefined("domain-failure-description")) {
                throw new RuntimeException(result.get("domain-failure-description").toString());
            }
            else if (result.hasDefined("host-failure-descriptions")) {
                throw new RuntimeException(result.get("host-failure-descriptions").toString());
            }
            else {
                throw new RuntimeException("Operation outcome is " + result.get("outcome").asString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        DomainLifecycleUtil starterUtil = new DomainLifecycleUtil(20000, InetAddress.getByName("127.0.0.1"), 9999) ;
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
