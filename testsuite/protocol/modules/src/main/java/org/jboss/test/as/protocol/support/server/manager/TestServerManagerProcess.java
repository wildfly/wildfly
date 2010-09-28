/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.test.as.protocol.support.server.manager;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.as.model.DomainModel;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.SystemExiter;
import org.jboss.as.process.ProcessOutputStreamHandler.Managed;
import org.jboss.as.server.manager.Main;
import org.jboss.as.server.manager.ServerManager;
import org.jboss.as.server.manager.ServerManagerEnvironment;
import org.jboss.test.as.protocol.support.process.NoopExiter;
import org.jboss.test.as.protocol.support.process.TestProcessManager;
import org.jboss.test.as.protocol.support.xml.ConfigParser;

/**
 * Starts a real server manager instance in-process
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TestServerManagerProcess extends ServerManager {

    private final Managed managed;

    private final CountDownLatch shutdownServersLatch = new CountDownLatch(1);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private volatile CountDownLatch downLatch = new CountDownLatch(1);

    public static ServerManager createServerManager(TestProcessManager pm) throws Exception{
        return createServerManager(pm, false);
    }

    public TestServerManagerProcess(Managed managed, ServerManagerEnvironment environment) {
        super(environment);
        this.managed = managed;
    }

    public static TestServerManagerProcess createServerManager(TestProcessManager pm, boolean restart) throws Exception{
        String[] args = new String[] {
                CommandLineConstants.INTERPROCESS_NAME,
                "ServerManager",
                CommandLineConstants.INTERPROCESS_PM_ADDRESS,
                InetAddress.getLocalHost().getHostAddress(),
                CommandLineConstants.INTERPROCESS_PM_PORT,
                String.valueOf(pm.getPort()),
                CommandLineConstants.INTERPROCESS_SM_ADDRESS,
                InetAddress.getLocalHost().getHostAddress(),
                CommandLineConstants.INTERPROCESS_SM_PORT,
                "0"
        };

        if (restart) {
            int length = args.length;
            args = Arrays.copyOf(args,  length + 1);
            args[length] = CommandLineConstants.RESTART_SERVER_MANAGER;
        }

        return TestServerManagerProcess.createServerManager(null, Arrays.asList(args), System.in, System.out, System.err);
    }

    public static TestServerManagerProcess createServerManager(Managed managed, List<String> command, InputStream stdin, PrintStream stdout, PrintStream stderr) throws Exception {
        SystemExiter.initialize(new NoopExiter());

        //The command created by TestProcessManager only contains the args
        String[] args = command.toArray(new String[command.size()]);

        ServerManagerEnvironment config = Main.determineEnvironment(args, System.getProperties(), stdin, stdout, stderr);
        if (config == null) {
            throw new RuntimeException("Could not determine SM environment");
        } else {
            TestServerManagerProcess manager = new TestServerManagerProcess(managed, config);
            manager.start();
            DomainModel domain = ConfigParser.parseDomain(new File(System.getProperty(ServerManagerEnvironment.DOMAIN_CONFIG_DIR)));
            manager.setDomain(domain);
            return manager;
        }
    }

    @Override
    public void shutdownServers() {
        shutdownServersLatch.countDown();
        super.shutdownServers();
    }

    @Override
    public void stop() {
        super.stop();
        shutdownLatch.countDown();
    }

    public void crashServerManager(int exitCode) {
        managed.processEnded(exitCode);
    }

    @Override
    public void downServer(String downServerName) {
        super.downServer(downServerName);
        downLatch.countDown();
    }

    public void resetDownLatch() {
        downLatch = new CountDownLatch(1);
    }

    public void waitForShutdownServers() throws InterruptedException {
        waitForLatch(shutdownServersLatch);
    }

    public void waitForShutdown() throws InterruptedException {
        waitForLatch(shutdownLatch);
    }

    public void waitForDown() throws InterruptedException {
        waitForLatch(downLatch);
    }

    private void waitForLatch(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Wait timed out");
        }
    }
}
