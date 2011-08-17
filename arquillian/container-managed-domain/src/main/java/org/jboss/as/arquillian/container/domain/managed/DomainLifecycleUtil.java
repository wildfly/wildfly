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

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.client.helpers.domain.ServerIdentity;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.dmr.ModelNode;

/**
 * Utility for controlling the lifecycle of a domain.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainLifecycleUtil {

    private static final ThreadFactory threadFactory = new AsyncThreadFactory();

    private final Logger log = Logger.getLogger(DomainLifecycleUtil.class.getName());

    private Process process;
    private Thread shutdownThread;

    private final JBossAsManagedConfiguration configuration;
    private DomainClient domainClient;
    private Map<ServerIdentity, ServerStatus> serverStatuses = new HashMap<ServerIdentity, ServerStatus>();
    private Map<ServerIdentity, MBeanServerConnectionProvider> jmxConnectionProviders = new HashMap<ServerIdentity, MBeanServerConnectionProvider>();
    private Map<ServerIdentity, MBeanServerConnection> jmxConnections = new HashMap<ServerIdentity, MBeanServerConnection>();
    private MBeanServerConnectionProvider[] providers;
    private ExecutorService executor;

    public DomainLifecycleUtil(final JBossAsManagedConfiguration configuration) {
        assert configuration != null : "configuration is null";
        this.configuration = configuration;
    }

    public void start() {
        try {
            configuration.validate();

            String jbossHomeDir = configuration.getJbossHome();

            final String additionalJavaOpts = System.getProperty("jboss.options");

            File modulesJar = new File(jbossHomeDir + "/jboss-modules.jar");
            if (modulesJar.exists() == false)
                throw new IllegalStateException("Cannot find: " + modulesJar);

            String javaHome = configuration.getJavaHome();
            String java = (javaHome != null) ?  javaHome + "/bin/java" : "java";

            File domainDir = configuration.getDomainDirectory() != null ? new File(configuration.getDomainDirectory()) : new File(new File(jbossHomeDir), "domain");
            String domainPath = domainDir.getAbsolutePath();

            final String modulePath;
            if(configuration.getModulePath() != null && !configuration.getModulePath().isEmpty()) {
                modulePath = configuration.getModulePath();
            } else {
                modulePath = jbossHomeDir + "/modules";
            }

            List<String> cmd = new ArrayList<String>();
            cmd.add(java);
            if (additionalJavaOpts != null) {
                for (String opt : additionalJavaOpts.split("\\s+")) {
                    cmd.add(opt);
                }
            }
            cmd.add("-Djboss.home.dir=" + jbossHomeDir);
            cmd.add("-Dorg.jboss.boot.log.file=" + domainPath + "/log/process-controller.log");
            cmd.add("-Dlogging.configuration=file:" + jbossHomeDir + "/domain/configuration/logging.properties");
            cmd.add("-jar");
            cmd.add(modulesJar.getAbsolutePath());
            cmd.add("-mp");
            cmd.add(modulePath);
            cmd.add("-logmodule");
            cmd.add("org.jboss.logmanager");
            //cmd.add("-jaxpmodule");
            //cmd.add("javax.xml.jaxp-provider");
            cmd.add("org.jboss.as.process-controller");
            cmd.add("-jboss-home");
            cmd.add(jbossHomeDir);
            cmd.add("-jvm");
            cmd.add(java);
            cmd.add("--");
            cmd.add("-Dorg.jboss.boot.log.file=" + domainPath + "/log/host-controller.log");
            cmd.add("-Dlogging.configuration=file:" + jbossHomeDir + "/domain/configuration/logging.properties");
            if (additionalJavaOpts != null) {
                for (String opt : additionalJavaOpts.split("\\s+")) {
                    cmd.add(opt);
                }
            }
            cmd.add("--");
            cmd.add("-default-jvm");
            cmd.add(java);
            if (configuration.getDomainConfigFile() != null) {
                cmd.add("-domain-config");
                cmd.add(configuration.getDomainConfigFile());
            }
            if (configuration.getHostConfigFile() != null) {
                cmd.add("-host-config");
                cmd.add(configuration.getHostConfigFile());
            }
            if (configuration.getHostCommandLineProperties() != null) {
                for (String opt : configuration.getHostCommandLineProperties().split("\\s+")) {
                    cmd.add(opt);
                }
            }
            if (configuration.getDomainDirectory() != null) {
                cmd.add("-Djboss.domain.base.dir=" + configuration.getDomainDirectory());
            }

            log.info("Starting container with: " + cmd.toString());
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            long start = System.currentTimeMillis();
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

            long timeout = configuration.getStartupTimeoutInSeconds() * 1000;

            boolean serversAvailable = false;
            while (timeout > 0 && serversAvailable == false) {

                serversAvailable = areServersStarted();

                if (!serversAvailable) {
                    Thread.sleep(100);
                    timeout -= 100;
                }
            }

            if (!serversAvailable) {
                throw new TimeoutException(String.format("Managed servers were not started within [%d] seconds", configuration.getStartupTimeoutInSeconds()));
            }

            log.info("All servers started in " + (System.currentTimeMillis() - start) + " ms");

            Map<ServerIdentity, MBeanServerConnection> connections = new HashMap<ServerIdentity, MBeanServerConnection>();
            for (Map.Entry<ServerIdentity, ServerStatus> entry : serverStatuses.entrySet()) {
                switch (entry.getValue()) {
                case STARTED:
                    connections.put(entry.getKey(), null);
                }
            }

            log.info("Awaiting mbeanServer connections for " + connections.keySet());

            int available = 0;
            int enabledCount = connections.size();
            while (timeout > 0 && available < enabledCount) {
                for (Map.Entry<ServerIdentity, MBeanServerConnection> entry : connections.entrySet()) {
                    if (entry.getValue() == null) {
                        try {
                            MBeanServerConnectionProvider provider = getMBeanServerConnectionProvider(entry.getKey());
                            MBeanServerConnection mbeanServer = provider == null ? null : provider.getConnection();
                            if (mbeanServer != null) {
                                connections.put(entry.getKey(), mbeanServer);
                                available++;
                            }
                        } catch (Exception ignore) {
                            log.severe(String.format("Failed accessing mbean server on %s: %s", entry.getKey(), ignore));
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
                throw new TimeoutException(String.format("Could not connect to the managed server's MBeanServer for servers %s within [%d] seconds", notStartedServers.toString(), configuration.getStartupTimeoutInSeconds()));
            }

            log.info("All containers available");
        } catch (Exception e) {
            throw new RuntimeException("Could not start container", e);
        }

    }

    public Future<Void> startAsync() {
        Callable<Void> c = new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                start();
                return null;
            }
        };

        return getExecutorService().submit(c);
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
        finally {
            safeCloseDomainClient();
            final ExecutorService exec = executor;
            if (exec != null) {
                exec.shutdownNow();
                executor = null;
            }
        }
    }

    public Future<Void> stopAsync() {
        Callable<Void> c = new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                stop();
                return null;
            }
        };

        return Executors.newSingleThreadExecutor(threadFactory).submit(c);
    }

    public synchronized DomainClient getDomainClient() {
        if (domainClient == null) {
            try {
                InetAddress managementAddress = InetAddress.getByName(configuration.getHostControllerManagementAddress());

                domainClient = DomainClient.Factory.create(managementAddress, configuration.getHostControllerManagementPort());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        return domainClient;
    }

    private synchronized ExecutorService getExecutorService() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(threadFactory);
        }
        return executor;
    }

    private boolean areServersStarted() {
        try {
            Map<ServerIdentity, ServerStatus> statuses = getServerStatuses();
            for (Map.Entry<ServerIdentity, ServerStatus> entry : statuses.entrySet()) {
                switch (entry.getValue()) {
                    case DISABLED:
                    case STARTED:
                        continue;
                    default:
                        return false;
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

    private synchronized void safeCloseDomainClient()  {
        if (domainClient != null) {
            try {
                domainClient.close();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Caught exception closing DomainClient", e);
            }
        }
    }

    private Map<ServerIdentity, ServerStatus> getServerStatuses() {

        Map<ServerIdentity, ServerStatus> result = new HashMap<ServerIdentity, ServerStatus>();
        ModelNode op = new ModelNode();
        op.get("operation").set("read-children-names");
        op.get("child-type").set("server-config");
        op.get("address").add("host", configuration.getHostName());
        ModelNode opResult = executeForResult(new OperationBuilder(op).build());
        Set<String> servers = new HashSet<String>();
        for (ModelNode server : opResult.asList()) {
            servers.add(server.asString());
        }
        for (String server : servers) {
            ModelNode address = new ModelNode();
            address.add("host", configuration.getHostName());
            address.add("server-config", server);
            String group = readAttribute("group", address).asString();
            ServerStatus status = Enum.valueOf(ServerStatus.class, readAttribute("status", address).asString());
            ServerIdentity id = new ServerIdentity(configuration.getHostName(), group, server);
            result.put(id, status);
        }

        return result;
    }

    private ModelNode readAttribute(String name, ModelNode address) {
        ModelNode op = new ModelNode();
        op.get("operation").set("read-attribute");
        op.get("address").set(address);
        op.get("name").set(name);
        return executeForResult(new OperationBuilder(op).build());
    }

    private ModelNode executeForResult(ModelNode op) {
        return executeForResult(new OperationBuilder(op).build());
    }

    private ModelNode executeForResult(Operation op) {
        try {
            ModelNode result = getDomainClient().execute(op);
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

    public static void main(String[] args) throws Exception {
        DomainLifecycleUtil starterUtil = new DomainLifecycleUtil(new JBossAsManagedConfiguration()) ;
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
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            final boolean writeOutput = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

                @Override
                public Boolean run() {
                    // this needs a better name
                    String val = System.getProperty("org.jboss.as.writeconsole");
                    return val == null || !"false".equals(val);
                }
            });
            String line = null;
            try {
                while((line = reader.readLine())!=null) {
                    if (writeOutput) {
                        System.out.println(line);
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

    private static final class AsyncThreadFactory implements ThreadFactory {

        private int threadCount;
        @Override
        public Thread newThread(Runnable r) {

            Thread t = new Thread(r, DomainLifecycleUtil.class.getSimpleName() + "-" + (++threadCount));
            t.setDaemon(true);
            return t;
        }
    }
}
