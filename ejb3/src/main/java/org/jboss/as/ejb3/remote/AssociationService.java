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
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.registry.Registry;

/**
 * The Jakarta Enterprise Beans server association service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AssociationService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "association");

    private final Consumer<AssociationService> associationServiceConsumer;
    private final Supplier<DeploymentRepository> deploymentRepositorySupplier;
    private final List<Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry<GroupMember, String, List<ClientMapping>>>>> registriesSupplier;

    private volatile AssociationImpl value;

    public AssociationService(final Consumer<AssociationService> associationServiceConsumer,
                              final Supplier<DeploymentRepository> deploymentRepositorySupplier,
                              final List<Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry<GroupMember, String, List<ClientMapping>>>>> registriesSupplier) {
        this.associationServiceConsumer = associationServiceConsumer;
        this.deploymentRepositorySupplier = deploymentRepositorySupplier;
        this.registriesSupplier = registriesSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        // todo suspendController
        List<Map.Entry<ProtocolSocketBinding, Registry<GroupMember, String, List<ClientMapping>>>> clientMappingsRegistries = this.registriesSupplier.isEmpty() ? Collections.emptyList() : new ArrayList<>(this.registriesSupplier.size());
        for (Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry<GroupMember, String, List<ClientMapping>>>> entry : this.registriesSupplier) {
            clientMappingsRegistries.add(new SimpleImmutableEntry<>(entry.getKey().get(), entry.getValue().get()));
        }
        value = new AssociationImpl(deploymentRepositorySupplier.get(), clientMappingsRegistries);
        this.associationServiceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        this.associationServiceConsumer.accept(null);
        value.close();
        value = null;
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

