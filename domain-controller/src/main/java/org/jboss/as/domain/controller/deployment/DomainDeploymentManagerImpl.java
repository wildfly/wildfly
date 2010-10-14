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

package org.jboss.as.domain.controller.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jboss.as.deployment.client.api.DuplicateDeploymentNameException;
import org.jboss.as.deployment.client.api.domain.DeploymentPlan;
import org.jboss.as.deployment.client.api.domain.DeploymentPlanResult;
import org.jboss.as.deployment.client.api.domain.DomainDeploymentManager;
import org.jboss.as.deployment.client.api.domain.InitialDeploymentPlanBuilder;
import org.jboss.as.deployment.client.impl.DeploymentContentDistributor;
import org.jboss.as.deployment.client.impl.domain.InitialDeploymentPlanBuilderFactory;
import org.jboss.as.deployment.client.impl.domain.DeploymentPlanImpl;
import org.jboss.as.domain.controller.DomainConfigurationPersister;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.model.DomainModel;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Default in-vm implementation of {@link DomainDeploymentManager}.
 *
 * @author Brian Stansberry
 */
public class DomainDeploymentManagerImpl implements DomainDeploymentManager, Service<DomainDeploymentManager> {

    private static Logger logger = Logger.getLogger("org.jboss.as.domain.deployment");

    private final DeploymentContentDistributor contentDistributor;
    private final ServiceContainer serviceContainer;
    private final InjectedValue<DomainController> injectedDomainController = new InjectedValue<DomainController>();
    private final InjectedValue<DomainDeploymentRepository> injectedDeploymentRepository = new InjectedValue<DomainDeploymentRepository>();
    private final InjectedValue<Executor> injectedDeploymentExecutor = new InjectedValue<Executor>();
    private final InjectedValue<DomainConfigurationPersister> injectedConfigurationPersister = new InjectedValue<DomainConfigurationPersister>();

    /**
     * Creates an instance of DomainDeploymentManagerImpl and configures the BatchBuilder to install it.
     *
     * @param serverConfiguration configuration of the server the service will manage. Cannot be {@code null}
     * @param serviceContainer the server's {@link ServiceContainer}. Cannot be {@code null}
     * @param batchBuilder service batch builder to use to install the service.  Cannot be {@code null}
     */
    public static void addService(final ServiceContainer serviceContainer, final BatchBuilder batchBuilder) {

        DomainDeploymentManagerImpl service = new DomainDeploymentManagerImpl(serviceContainer);
        batchBuilder.addService(SERVICE_NAME_LOCAL, service)
            .addDependency(DomainController.SERVICE_NAME, DomainController.class, service.injectedDomainController)
            .addDependency(DomainDeploymentRepository.SERVICE_NAME, DomainDeploymentRepository.class, service.injectedDeploymentRepository)
            .addDependency(DomainConfigurationPersister.SERVICE_NAME, DomainConfigurationPersister.class, service.injectedConfigurationPersister);

        // FIXME inject Executor from an external service dependency
        final Executor hack = Executors.newCachedThreadPool();
        service.injectedDeploymentExecutor.inject(hack);
    }

    public DomainDeploymentManagerImpl(final ServiceContainer serviceContainer) {

        if (serviceContainer == null)
            throw new IllegalArgumentException("serviceContainer is null");
        this.serviceContainer = serviceContainer;

        this.contentDistributor = new DeploymentContentDistributor() {
            @Override
            public byte[] distributeDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException, DuplicateDeploymentNameException {
                if (DomainDeploymentManagerImpl.this.getDomainModel().getDeployment(name) != null) {
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

    @Override
    public Future<DeploymentPlanResult> execute(DeploymentPlan plan) {

        if (!(plan instanceof DeploymentPlanImpl)) {
            throw new IllegalArgumentException("unexpected " + DeploymentPlan.class.getSimpleName() + " type " + plan.getClass().getName());
        }

        final DeploymentPlanImpl planImpl = (DeploymentPlanImpl) plan;

        final SimpleFuture<DeploymentPlanResult> resultFuture = new SimpleFuture<DeploymentPlanResult>();

        // TODO create a task to execute
        // Execute the plan asynchronously
        Runnable r = new Runnable() {
            @Override
            public void run() {
                logger.debugf("Executing deployment plan %s", planImpl.getId().toString());
                // TODO execute
                throw new UnsupportedOperationException("implement me");
            }
        };
        getDeploymentExecutor().execute(r);

        throw new UnsupportedOperationException("implement me");
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
            type = DomainConfigurationPersister.class.getSimpleName();
            injectedConfigurationPersister.getValue();
            type = Executor.class.getSimpleName();
            injectedDeploymentExecutor.getValue();
            type = DomainDeploymentRepository.class.getSimpleName();
            injectedDeploymentRepository.getValue();
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
    public DomainDeploymentManager getValue() throws IllegalStateException {
        return this;
    }

    private DomainModel getDomainModel() {
        return this.injectedDomainController.getValue().getDomainModel();
    }

    private Executor getDeploymentExecutor() {
        return injectedDeploymentExecutor.getValue();
    }

    private DeploymentContentDistributor getDeploymentContentDistributor() {
        return contentDistributor;
    }

    private DomainDeploymentRepository getDeploymentRepository() {
        return injectedDeploymentRepository.getValue();
    }

    private ServiceContainer getServiceContainer() {
        return serviceContainer;
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

}
