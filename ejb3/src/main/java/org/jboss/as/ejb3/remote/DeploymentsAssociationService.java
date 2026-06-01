/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.ProtocolSocketBinding;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.registry.Registry;

/**
 * A service providing an instance of Association to be used when deployments are available.
 *
 * @author Richard Achmatowicz
 */
public final class DeploymentsAssociationService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "association","deployments");

    private final DelegatingAssociationImpl delegator;
    private final Supplier<DeploymentRepository> deploymentRepositorySupplier;
    private final Supplier<Executor> executorSupplier;
    private final List<Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry<GroupMember, String, List<ClientMapping>>>>> registriesSupplier;

    private volatile DeploymentsAssociationImpl value;

    public DeploymentsAssociationService(final DelegatingAssociationImpl delegator,
                                         final Supplier<DeploymentRepository> deploymentRepositorySupplier,
                                         final Supplier<Executor> executorSupplier,
                              final List<Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry<GroupMember, String, List<ClientMapping>>>>> registriesSupplier) {
        this.delegator = delegator;
        this.deploymentRepositorySupplier = deploymentRepositorySupplier;
        this.executorSupplier = executorSupplier;
        this.registriesSupplier = registriesSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("Starting DeploymentsAssociationService");

         // todo suspendController
        List<Map.Entry<ProtocolSocketBinding, Registry<GroupMember, String, List<ClientMapping>>>> clientMappingsRegistries = this.registriesSupplier.isEmpty() ? Collections.emptyList() : new ArrayList<>(this.registriesSupplier.size());
        for (Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry<GroupMember, String, List<ClientMapping>>>> entry : this.registriesSupplier) {
            clientMappingsRegistries.add(new SimpleImmutableEntry<>(entry.getKey().get(), entry.getValue().get()));
        }
        value = new DeploymentsAssociationImpl(deploymentRepositorySupplier.get(), executorSupplier.get(),  clientMappingsRegistries);

        // swap the current association implementation for this one
        delegator.accept(value);

        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("Started DeploymentsAssociationService");
    }

    @Override
    public void stop(final StopContext context) {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("Stopping DeploymentsAssociationService");

        // if we are moving from AssociationImpl to NoDeploymentsAssociationImpl, we may be the last node to leave
        value.sendTopologyUpdateIfLastNodeToLeave();

        // swap this association implementation for the other
        delegator.accept(NoDeploymentsAssociationImpl.INSTANCE);

        value.close();
        value = null;

        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("Stopped DeploymentsAssociationService");
    }
}

