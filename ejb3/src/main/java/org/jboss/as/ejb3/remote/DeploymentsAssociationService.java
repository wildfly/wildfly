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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.ProtocolSocketBinding;
import org.jboss.ejb.server.Association;
import org.jboss.logging.Logger;
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

    public static final Logger logger = Logger.getLogger("org.jboss.as.ejb.remote.DeploymentsAssociationService");

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "association","deployments");

    private final Consumer<DeploymentsAssociationService> deploymentsAssociationServiceConsumer;
    private final Supplier<AssociationService> associationServiceSupplier;
    private final Supplier<DeploymentRepository> deploymentRepositorySupplier;
    private final List<Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry>>> registriesSupplier;

    private volatile DeploymentsAssociationImpl value;

    public DeploymentsAssociationService(final Consumer<DeploymentsAssociationService> deploymentsAssociationServiceConsumer,
                                         final Supplier<AssociationService> associationServiceSupplier,
                                         final Supplier<DeploymentRepository> deploymentRepositorySupplier,
                              final List<Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry>>> registriesSupplier) {
        this.deploymentsAssociationServiceConsumer = deploymentsAssociationServiceConsumer;
        this.associationServiceSupplier = associationServiceSupplier;
        this.deploymentRepositorySupplier = deploymentRepositorySupplier;
        this.registriesSupplier = registriesSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        logger.trace("Starting service");
        // todo suspendController
        List<Map.Entry<ProtocolSocketBinding, Registry<GroupMember, String, List<ClientMapping>>>> clientMappingsRegistries = this.registriesSupplier.isEmpty() ? Collections.emptyList() : new ArrayList<>(this.registriesSupplier.size());
        for (Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry>> entry : this.registriesSupplier) {
            clientMappingsRegistries.add(new SimpleImmutableEntry<>(entry.getKey().get(), entry.getValue().get()));
        }
        value = new DeploymentsAssociationImpl(deploymentRepositorySupplier.get(), clientMappingsRegistries);

        // swap the current association implementation for this one
        associationServiceSupplier.get().getDelegator().accept(value);

        // make our service available to dependants
        deploymentsAssociationServiceConsumer.accept(this);
        logger.trace("Started service");
    }

    @Override
    public void stop(final StopContext context) {
        logger.trace("Stopping service");
        // remove our service availability from dependants
        deploymentsAssociationServiceConsumer.accept(null);

        // swap this association implementation for the other
        associationServiceSupplier.get().getDelegator().accept(NoDeploymentsAssociationImpl.INSTANCE);

        value.close();
        value = null;
        logger.trace("Stopped service");
    }

    public Association getAssociation() {
        return value;
    }

    void setExecutor(Executor executor) {
        this.value.setExecutor(executor);
    }

    void sendTopologyUpdateIfLastNodeToLeave() {
        this.value.sendTopologyUpdateIfLastNodeToLeave();
    }
}

