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

package org.jboss.as.server.deployment.scanner;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

/**
 * Service responsible creating a {@code DeploymentScanner}
 *
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerService implements Service<DeploymentScanner> {

    private static final int DEFAULT_INTERVAL = 5000;
    private long interval;
    private TimeUnit unit = TimeUnit.MILLISECONDS;
    private boolean enabled;
    private boolean autoDeployZipped;
    private boolean autoDeployExploded;
    private boolean autoDeployXml;
    private Long deploymentTimeout;
    private final String relativeTo;
    private final String path;

    /**
     * The created scanner.
     */
    private DeploymentScanner scanner;

    private final InjectedValue<PathManager> pathManagerValue = new InjectedValue<PathManager>();
    private final InjectedValue<ModelController> controllerValue = new InjectedValue<ModelController>();
    private final InjectedValue<ScheduledExecutorService> scheduledExecutorValue = new InjectedValue<ScheduledExecutorService>();
    private volatile PathManager.Callback.Handle callbackHandle;

    public static ServiceName getServiceName(String repositoryName) {
        return DeploymentScanner.BASE_SERVICE_NAME.append(repositoryName);
    }

    /**
     * Add the deployment scanner service to a batch.
     *
     * @param serviceTarget     the service target
     * @param name              the repository name
     * @param relativeTo        the relative to
     * @param path              the path
     * @param scanInterval      the scan interval
     * @param scanEnabled       scan enabled
     * @param deploymentTimeout the deployment timeout
     * @param bootTimeService   the deployment scanner used in the boot time scan
     * @return
     */
    public static ServiceController<DeploymentScanner> addService(final ServiceTarget serviceTarget, final String name, final String relativeTo, final String path,
                                                                  final Integer scanInterval, TimeUnit unit, final Boolean autoDeployZip,
                                                                  final Boolean autoDeployExploded, final Boolean autoDeployXml, final Boolean scanEnabled, final Long deploymentTimeout,
                                                                  final List<ServiceController<?>> newControllers, final FileSystemDeploymentService bootTimeService, final ScheduledExecutorService scheduledExecutorService,
                                                                  final ServiceListener<Object>... listeners) {
        final DeploymentScannerService service = new DeploymentScannerService(relativeTo, path, scanInterval, unit, autoDeployZip, autoDeployExploded, autoDeployXml, scanEnabled, deploymentTimeout, bootTimeService);
        final ServiceName serviceName = getServiceName(name);

        ServiceBuilder<DeploymentScanner> builder = serviceTarget.addService(serviceName, service)
                .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.pathManagerValue)
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.controllerValue)
                .addDependency(org.jboss.as.server.deployment.Services.JBOSS_DEPLOYMENT_CHAINS)
                .addInjection(service.scheduledExecutorValue, scheduledExecutorService);
        builder.addListener(listeners);
        ServiceController<DeploymentScanner> svc = builder.setInitialMode(Mode.ACTIVE).install();
        if (newControllers != null) {
            newControllers.add(svc);
        }
        return svc;
    }

    DeploymentScannerService(final String relativeTo, final String path, final Integer interval, final TimeUnit unit, final Boolean autoDeployZipped,
                             final Boolean autoDeployExploded, final Boolean autoDeployXml, final Boolean enabled, final Long deploymentTimeout, final FileSystemDeploymentService bootTimeService) {
        this.relativeTo = relativeTo;
        this.path = path;
        this.interval = interval == null ? DEFAULT_INTERVAL : interval.longValue();
        this.unit = unit;
        this.autoDeployZipped = autoDeployZipped == null ? true : autoDeployZipped.booleanValue();
        this.autoDeployExploded = autoDeployExploded == null ? false : autoDeployExploded.booleanValue();
        this.autoDeployXml = autoDeployXml == null ? true : autoDeployXml.booleanValue();
        this.enabled = enabled == null ? true : enabled.booleanValue();
        this.deploymentTimeout = deploymentTimeout;
        this.scanner = bootTimeService;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            //if this is the first start we want to use the same scanner that was used at boot time
            if (scanner == null) {
                final PathManager pathManager = pathManagerValue.getValue();
                final String pathName = pathManager.resolveRelativePathEntry(path, relativeTo);
                File relativePath = null;
                if (relativeTo != null) {
                    relativePath = new File(pathManager.getPathEntry(relativeTo).resolvePath());
                    callbackHandle = pathManager.registerCallback(pathName, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
                }


                final FileSystemDeploymentService scanner = new FileSystemDeploymentService(relativeTo, new File(pathName), relativePath, scheduledExecutorValue.getValue());

                scanner.setScanInterval(unit.toMillis(interval));
                scanner.setAutoDeployExplodedContent(autoDeployExploded);
                scanner.setAutoDeployZippedContent(autoDeployZipped);
                scanner.setAutoDeployXMLContent(autoDeployXml);
                if (deploymentTimeout != null) {
                    scanner.setDeploymentTimeout(deploymentTimeout);
                }
                this.scanner = scanner;
            }
            if (enabled) {
                scanner.startScanner(new DefaultDeploymentOperations(controllerValue.getValue().createClient(scheduledExecutorValue.getValue())));
            }
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop(StopContext context) {
        final DeploymentScanner scanner = this.scanner;
        this.scanner = null;
        scanner.stopScanner();
        scheduledExecutorValue.getValue().shutdown();
        if (callbackHandle != null) {
            callbackHandle.remove();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized DeploymentScanner getValue() throws IllegalStateException {
        final DeploymentScanner scanner = this.scanner;
        if (scanner == null) {
            throw new IllegalStateException();
        }
        return scanner;
    }

    static class DefaultDeploymentOperations implements DeploymentOperations {

        private final ModelControllerClient controllerClient;

        DefaultDeploymentOperations(final ModelControllerClient controllerClient) {
            this.controllerClient = controllerClient;
        }

        @Override
        public Future<ModelNode> deploy(final ModelNode operation, final ScheduledExecutorService scheduledExecutor) {
            return scheduledExecutor.submit(new Callable<ModelNode>() {
                @Override
                public ModelNode call() throws Exception {
                    return controllerClient.execute(operation);
                }
            });
        }

        @Override
        public Set<String> getDeploymentNames() {
            final ModelNode op = Util.getEmptyOperation(READ_CHILDREN_NAMES_OPERATION, new ModelNode());
            op.get(CHILD_TYPE).set(DEPLOYMENT);
            ModelNode response;
            try {
                response = controllerClient.execute(op);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            final ModelNode result = response.get(RESULT);
            final Set<String> deploymentNames = new HashSet<String>();
            if (result.isDefined()) {
                final List<ModelNode> deploymentNodes = result.asList();
                for (ModelNode node : deploymentNodes) {
                    deploymentNames.add(node.asString());
                }
            }
            return deploymentNames;
        }

        @Override
        public void close() throws IOException {
            controllerClient.close();
        }
    }

}
