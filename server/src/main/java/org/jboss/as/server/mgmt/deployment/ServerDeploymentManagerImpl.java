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

package org.jboss.as.server.mgmt.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.as.deployment.ServerDeploymentRepository;
import org.jboss.as.deployment.client.api.DuplicateDeploymentNameException;
import org.jboss.as.deployment.client.api.server.AbstractServerUpdateActionResult;
import org.jboss.as.deployment.client.api.server.DeploymentPlan;
import org.jboss.as.deployment.client.api.server.InitialDeploymentPlanBuilder;
import org.jboss.as.deployment.client.api.server.ServerDeploymentActionResult;
import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.deployment.client.api.server.ServerDeploymentPlanResult;
import org.jboss.as.deployment.client.api.server.SimpleServerDeploymentActionResult;
import org.jboss.as.deployment.client.api.server.ServerUpdateActionResult.Result;
import org.jboss.as.deployment.client.impl.DeploymentActionImpl;
import org.jboss.as.deployment.client.impl.DeploymentContentDistributor;
import org.jboss.as.deployment.client.impl.server.DeploymentPlanImpl;
import org.jboss.as.deployment.client.impl.server.DeploymentPlanResultImpl;
import org.jboss.as.deployment.client.impl.server.InitialDeploymentPlanBuilderFactory;
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.ServerModelDeploymentAdd;
import org.jboss.as.model.ServerModelDeploymentFullReplaceUpdate;
import org.jboss.as.model.ServerModelDeploymentRemove;
import org.jboss.as.model.ServerModelDeploymentReplaceUpdate;
import org.jboss.as.model.ServerModelDeploymentStartStopUpdate;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.mgmt.ServerConfigurationPersister;
import org.jboss.as.server.mgmt.ServerUpdateController;
import org.jboss.as.server.mgmt.ShutdownHandler;
import org.jboss.as.server.mgmt.SimpleFuture;
import org.jboss.as.server.mgmt.ServerUpdateController.ServerUpdateCommitHandler;
import org.jboss.as.server.mgmt.ServerUpdateController.Status;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Default in-vm implementation of {@link ServerDeploymentManager}.
 *
 * @author Brian Stansberry
 */
public class ServerDeploymentManagerImpl implements ServerDeploymentManager, Service<ServerDeploymentManager> {

    private static Logger logger = Logger.getLogger("org.jboss.as.server.deployment");

    private final ServerModel serverConfiguration;
    private final ServiceContainer serviceContainer;
    private final InjectedValue<ServerDeploymentRepository> injectedDeploymentRepository = new InjectedValue<ServerDeploymentRepository>();
    private final InjectedValue<Executor> injectedDeploymentExecutor = new InjectedValue<Executor>();
    private final InjectedValue<ServerConfigurationPersister> injectedConfigurationPersister = new InjectedValue<ServerConfigurationPersister>();
    private final InjectedValue<ShutdownHandler> injectedShutdownHandler = new InjectedValue<ShutdownHandler>();
    private final DeploymentContentDistributor contentDistributor;

    /**
     * Creates an instance of StandaloneDeploymentManagerImpl and configures the BatchBuilder to install it.
     *
     * @param serverConfiguration configuration of the server the service will manage. Cannot be {@code null}
     * @param serviceContainer the server's {@link ServiceContainer}. Cannot be {@code null}
     * @param batchBuilder service batch builder to use to install the service.  Cannot be {@code null}
     */
    public static void addService(ServerModel serverConfiguration, ServiceContainer serviceContainer, BatchBuilder batchBuilder) {

        ServerDeploymentManagerImpl service = new ServerDeploymentManagerImpl(serverConfiguration, serviceContainer);
        batchBuilder.addService(SERVICE_NAME_LOCAL, service)
            .addDependency(ServerDeploymentRepository.SERVICE_NAME, ServerDeploymentRepository.class, service.injectedDeploymentRepository)
            .addDependency(ServerConfigurationPersister.SERVICE_NAME, ServerConfigurationPersister.class, service.injectedConfigurationPersister)
            .addDependency(ShutdownHandler.SERVICE_NAME, ShutdownHandler.class, service.injectedShutdownHandler);

        // FIXME inject Executor from an external service dependency
        final Executor hack = Executors.newCachedThreadPool();
        service.injectedDeploymentExecutor.inject(hack);
    }

    /**
     * Creates a new StandaloneDeploymentManagerImpl.
     *
     * @param serverConfiguration the server's configuration model. Cannot be <code>null</code>
     * @param serviceContainer the server's service container. Cannot be <code>null</code>
     *
     * @throws IllegalArgumentException if a required parameter is <code>null</code>
     */
    public ServerDeploymentManagerImpl(final ServerModel serverConfiguration, ServiceContainer serviceContainer) {
        if (serverConfiguration == null)
            throw new IllegalArgumentException("serverConfiguration is null");
        if (serviceContainer == null)
            throw new IllegalArgumentException("serviceContainer is null");
        this.serverConfiguration = serverConfiguration;
        this.serviceContainer = serviceContainer;

        this.contentDistributor = new DeploymentContentDistributor() {
            @Override
            public byte[] distributeDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException, DuplicateDeploymentNameException {
                if (ServerDeploymentManagerImpl.this.serverConfiguration.getDeployment(name) != null) {
                    throw new DuplicateDeploymentNameException(name, false);
                }
                return getDeploymentRepository().addDeploymentContent(name, runtimeName, stream);
            }
            @Override
            public byte[] distributeReplacementDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException {
                return getDeploymentRepository().addDeploymentContent(name, runtimeName, stream);
            }
        };
    }

    // ServerDeploymentManager implementation

    @Override
    public String addDeploymentContent(File file) throws IOException, DuplicateDeploymentNameException {
        String name = file.getName();
        getDeploymentContentDistributor().distributeDeploymentContent(name, name, new FileInputStream(file));
        return name;
    }

    @Override
    public String addDeploymentContent(URL url) throws IOException, DuplicateDeploymentNameException {
        String name = getName(url);
        addDeploymentContent(name, name, url);
        return name;
    }

    @Override
    public void addDeploymentContent(String name, File file) throws IOException, DuplicateDeploymentNameException {
        String commonName = file.getName();
        getDeploymentContentDistributor().distributeDeploymentContent(name, commonName, new FileInputStream(file));
    }

    @Override
    public void addDeploymentContent(String name, URL url) throws IOException, DuplicateDeploymentNameException {
        String commonName = getName(url);
        addDeploymentContent(name, commonName , url);
    }

    private void addDeploymentContent(String name, String commonName, URL url) throws IOException, DuplicateDeploymentNameException {
        URLConnection conn = url.openConnection();
        conn.connect();
        getDeploymentContentDistributor().distributeDeploymentContent(name, commonName, conn.getInputStream());
    }

    @Override
    public void addDeploymentContent(String name, InputStream stream) throws IOException, DuplicateDeploymentNameException {
        addDeploymentContent(name, name, stream);
    }

    @Override
    public void addDeploymentContent(String name, String commonName, InputStream stream) throws IOException, DuplicateDeploymentNameException {
        getDeploymentContentDistributor().distributeDeploymentContent(name, commonName, stream);
    }

    @Override
    public Future<ServerDeploymentPlanResult> execute(final DeploymentPlan plan) {

        if (!(plan instanceof DeploymentPlanImpl)) {
            throw new IllegalArgumentException("unexpected " + DeploymentPlan.class.getSimpleName() + " type " + plan.getClass().getName());
        }


        final SimpleFuture<ServerDeploymentPlanResult> resultFuture = new SimpleFuture<ServerDeploymentPlanResult>();
        final UpdateResultHandlerImpl resultHandler = new UpdateResultHandlerImpl(resultFuture, plan);
        final ServerUpdateController controller = new ServerUpdateController(getServerConfiguration(), getServiceContainer(),
                getDeploymentExecutor(), resultHandler, plan.isGlobalRollback(), !plan.isShutdown());

        DeploymentPlanImpl planImpl = (DeploymentPlanImpl) plan;

        for (DeploymentActionImpl action : planImpl.getDeploymentActionImpls()) {
            addServerGroupDeploymentUpdate(action, resultHandler, controller);
        }

        // Execute the plan asynchronously
        Runnable r = new Runnable() {
            @Override
            public void run() {
                logger.debugf("Executing deployment plan %s", plan.getId().toString());
                controller.executeUpdates();
            }
        };
        getDeploymentExecutor().execute(r);

        return resultFuture;
    }

    @Override
    public InitialDeploymentPlanBuilder newDeploymentPlan() {
        return InitialDeploymentPlanBuilderFactory.newInitialDeploymentPlanBuilder(getDeploymentContentDistributor());
    }

    // Service implementation

    @Override
    public void start(StartContext context) throws StartException {

        // Verify injections
        String type = null;
        try {
            type = ServerConfigurationPersister.class.getSimpleName();
            injectedConfigurationPersister.getValue();
            type = Executor.class.getSimpleName();
            injectedDeploymentExecutor.getValue();
            type = ServerDeploymentRepository.class.getSimpleName();
            injectedDeploymentRepository.getValue();
            type = ShutdownHandler.class.getSimpleName();
            injectedShutdownHandler.getValue();
        }
        catch (IllegalStateException ise) {
            throw new StartException(type + " was not injected");
        }

    }

    @Override
    public void stop(StopContext context) {
        // no-op
    }

    @Override
    public ServerDeploymentManager getValue() throws IllegalStateException {
        return this;
    }

    // Private

    private ServerModel getServerConfiguration() {
        return this.serverConfiguration;
    }

    private Executor getDeploymentExecutor() {
        return injectedDeploymentExecutor.getValue();
    }

    private DeploymentContentDistributor getDeploymentContentDistributor() {
        return contentDistributor;
    }

    private ServerDeploymentRepository getDeploymentRepository() {
        return injectedDeploymentRepository.getValue();
    }

    private ServiceContainer getServiceContainer() {
        return this.serviceContainer;
    }

    private ShutdownHandler getShutdownHandler() {
        return injectedShutdownHandler.getValue();
    }

    /**
     * Creates an update object for the given action and adds it to the overall
     * update.
     * @param action the action
     * @param resultHandler the handler for the result of the action
     * @param overallUpdate the overall update
     */
    private void addServerGroupDeploymentUpdate(DeploymentActionImpl action, UpdateResultHandler<? super ServerDeploymentActionResult, UUID> resultHandler, final ServerUpdateController controller) {

        switch (action.getType()) {
            case ADD: {
                controller.addServerModelUpdate(new ServerModelDeploymentAdd(action.getDeploymentUnitUniqueName(), action.getNewContentFileName(), action.getNewContentHash()), resultHandler, action.getId());
                break;
            }
            case REMOVE: {
                controller.addServerModelUpdate(new ServerModelDeploymentRemove(action.getDeploymentUnitUniqueName()), resultHandler, action.getId());
                break;
            }
            case DEPLOY: {
                controller.addServerModelUpdate(new ServerModelDeploymentStartStopUpdate(action.getDeploymentUnitUniqueName(), true), resultHandler, action.getId());
                break;
            }
            case UNDEPLOY: {
                controller.addServerModelUpdate(new ServerModelDeploymentStartStopUpdate(action.getDeploymentUnitUniqueName(), false), resultHandler, action.getId());
                break;
            }
            case REDEPLOY: {
                controller.addServerModelUpdate(new ServerModelDeploymentReplaceUpdate(action.getDeploymentUnitUniqueName(), action.getDeploymentUnitUniqueName()), resultHandler, action.getId());
                break;
            }
            case REPLACE: {
                controller.addServerModelUpdate(new ServerModelDeploymentReplaceUpdate(action.getDeploymentUnitUniqueName(), action.getReplacedDeploymentUnitUniqueName()), resultHandler, action.getId());
                break;
            }
            case FULL_REPLACE:
                ServerGroupDeploymentElement deployment = serverConfiguration.getDeployment(action.getDeploymentUnitUniqueName());
                boolean redeploy = deployment != null && deployment.isStart();
                controller.addServerModelUpdate(new ServerModelDeploymentFullReplaceUpdate(action.getDeploymentUnitUniqueName(), action.getNewContentFileName(), action.getNewContentHash(), redeploy), resultHandler, action.getId());
                break;
            default: {
                throw new IllegalStateException("Unknown type " + action.getType());
            }
        }
    }

    private ServerConfigurationPersister getConfigurationPersister() {
        return injectedConfigurationPersister.getValue();
    }

    private static String getName(URL url) {
        if ("file".equals(url.getProtocol())) {
            try {
                File f = new File(url.toURI());
                return f.getName();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(url + " is not a valid URI", e);
            }
        }

        String path = url.getPath();
        int idx = path.lastIndexOf('/');
        while (idx == path.length() - 1) {
            path = path.substring(0, idx);
            idx = path.lastIndexOf('/');
        }
        if (idx == -1) {
            throw new IllegalArgumentException("Cannot derive a deployment name from " +
                    url + " -- use an overloaded method variant that takes a 'name' parameter");
        }

        return path.substring(idx + 1);
    }

    private class UpdateResultHandlerImpl implements UpdateResultHandler<Object, UUID>, ServerUpdateCommitHandler {

        private final Map<UUID, ServerDeploymentActionResult> updateResults = new HashMap<UUID, ServerDeploymentActionResult>();
        private final Set<UUID> successfulRollbacks = new HashSet<UUID>();
        private final Map<UUID, Throwable> failedRollbacks = new HashMap<UUID, Throwable>();


        private final SimpleFuture<ServerDeploymentPlanResult> future;
        private final DeploymentPlan plan;

        private UpdateResultHandlerImpl(final SimpleFuture<ServerDeploymentPlanResult> future, final DeploymentPlan plan) {
            this.future = future;
            this.plan = plan;
        }

        @Override
        public void handleCancellation(UUID param) {
            synchronized (updateResults) {
                // FIXME we need to clarify the semantics of a cancellation
                updateResults.put(param, new SimpleServerDeploymentActionResult(param, Result.NOT_EXECUTED));
            }
        }

        @Override
        public void handleFailure(Throwable cause, UUID param) {
            synchronized (updateResults) {
                updateResults.put(param, new SimpleServerDeploymentActionResult(param, cause));
            }
        }

        @Override
        public void handleRollbackCancellation(UUID param) {
            synchronized (failedRollbacks) {
                failedRollbacks.put(param, null);
            }
        }

        @Override
        public void handleRollbackFailure(Throwable cause, UUID param) {
            synchronized (failedRollbacks) {
                failedRollbacks.put(param, cause);
            }
        }

        @Override
        public void handleRollbackSuccess(UUID param) {
            synchronized (successfulRollbacks) {
                successfulRollbacks.add(param);
            }
        }

        @Override
        public void handleRollbackTimeout(UUID param) {
            synchronized (failedRollbacks) {
                failedRollbacks.put(param, null);
            }
        }

        @Override
        public void handleSuccess(Object result, UUID param) {
            synchronized (updateResults) {
                if (result instanceof ServerDeploymentActionResult) {
                    updateResults.put(param, (ServerDeploymentActionResult) result);
                }
                else {
                    updateResults.put(param, new SimpleServerDeploymentActionResult(param, Result.EXECUTED));
                }
            }
        }

        @Override
        public void handleTimeout(UUID param) {
            synchronized (updateResults) {
                // FIXME clarify meaning of "timeout"
                updateResults.put(param, new SimpleServerDeploymentActionResult(param, Result.FAILED));
            }
        }

        @Override
        public void handleUpdateCommit(ServerUpdateController controller,
                org.jboss.as.server.mgmt.ServerUpdateController.Status priorStatus) {
            getConfigurationPersister().configurationModified();

            generateResult(priorStatus);

            if (plan.isShutdown()) {
                if (plan.getGracefulShutdownTimeout() > -1) {
                    getShutdownHandler().gracefulShutdownRequested(plan.getGracefulShutdownTimeout(), TimeUnit.MILLISECONDS);
                }
                else {
                    getShutdownHandler().shutdownRequested();
                }
            }

        }

        private void generateResult(Status status) {

            Map<UUID, ServerDeploymentActionResult> planResults = new HashMap<UUID, ServerDeploymentActionResult>();
            for (Map.Entry<UUID, ServerDeploymentActionResult> entry : updateResults.entrySet()) {
                ServerDeploymentActionResult actionResult = entry.getValue();
                if (actionResult == null) {
                    // Treat as success
                    actionResult = new SimpleServerDeploymentActionResult(entry.getKey(), Result.EXECUTED);
                }
                if (actionResult.getResult() != Result.NOT_EXECUTED) {
                    ServerDeploymentActionResult rollbackResult = null;
                    if (successfulRollbacks.contains(entry.getKey())) {
                        rollbackResult = new SimpleServerDeploymentActionResult(entry.getKey(), Result.EXECUTED);
                    }
                    else if (failedRollbacks.containsKey(entry.getKey())) {
                        Throwable cause = failedRollbacks.get(entry.getKey());
                        rollbackResult = new SimpleServerDeploymentActionResult(entry.getKey(), cause);
                    }
                    if (rollbackResult != null) {
                        AbstractServerUpdateActionResult.installRollbackResult((AbstractServerUpdateActionResult<ServerDeploymentActionResult>) entry.getValue(), rollbackResult);
                    }
                }

                planResults.put(entry.getKey(), actionResult);
            }
            DeploymentPlanResultImpl result = new DeploymentPlanResultImpl(plan.getId(), planResults);

            future.set(result);
        }

    }

}
