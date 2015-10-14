/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jdr;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossThreadFactory;

import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.security.AccessController.doPrivileged;
import org.jboss.as.cli.scriptsupport.CLI;

/**
 * Service that provides a {@link JdrReportCollector}.
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 * @author Jesse Jaggars
 */
public class JdrReportService implements JdrReportCollector, Service<JdrReportCollector> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jdr", "collector");

    public static ServiceController<JdrReportCollector> addService(final ServiceTarget target) {

        JdrReportService service = new JdrReportService();
        return target.addService(SERVICE_NAME, service)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.serverEnvironmentValue)
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.modelControllerValue)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private final InjectedValue<ServerEnvironment> serverEnvironmentValue = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();
    private ExecutorService executorService;
    private ServerEnvironment serverEnvironment;
    private ModelControllerClient controllerClient;

    /**
     * Collect a JDR report when run outside the Application Server.
     * @param cli
     */
    public JdrReport standaloneCollect(CLI cli, String protocol, String host, int port) throws OperationFailedException {
        return new JdrRunner(cli, protocol, host, port, null, null).collect();
    }

    /**
     * Collect a JDR report.
     */
    public JdrReport collect() throws OperationFailedException {
        JdrRunner runner = new JdrRunner(true);
        serverEnvironment = serverEnvironmentValue.getValue();
        runner.setJbossHomeDir(serverEnvironment.getHomeDir().getAbsolutePath());
        runner.setReportLocationDir(serverEnvironment.getServerTempDir().getAbsolutePath());
        runner.setControllerClient(controllerClient);
        runner.setHostControllerName(serverEnvironment.getHostControllerName());
        runner.setServerName(serverEnvironment.getServerName());
        return runner.collect();
    }

    public synchronized void start(StartContext context) throws StartException {
        final ThreadFactory threadFactory = doPrivileged(new PrivilegedAction<JBossThreadFactory>() {
            public JBossThreadFactory run() {
                return new JBossThreadFactory(new ThreadGroup("JdrReportCollector-threads"), Boolean.FALSE, null, "%G - %t", null, null);
            }
        });
        executorService = Executors.newCachedThreadPool(threadFactory);
        serverEnvironment = serverEnvironmentValue.getValue();
        controllerClient = modelControllerValue.getValue().createClient(executorService);
    }

    public synchronized void stop(StopContext context) {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    public JdrReportService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
