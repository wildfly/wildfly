/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.immutable.Immutability;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.elytron.web.undertow.server.servlet.ServletSecurityContextImpl.IdentityContainer;
import org.wildfly.security.cache.CachedIdentity;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.servlet.util.SavedRequest;

/**
 * @author Paul Ferraro
 */
public class SessionManagerFactoryConfigurationAdapter<C extends DistributableSessionManagementConfiguration<DeploymentUnit>> extends WebDeploymentConfigurationAdapter implements org.wildfly.clustering.session.SessionManagerFactoryConfiguration<Map<String, Object>> {

    private final OptionalInt maxActiveSessions;
    private final ByteBufferMarshaller marshaller;
    private final Immutability immutability;
    private final SessionAttributePersistenceStrategy attributePersistenceStrategy;

    public SessionManagerFactoryConfigurationAdapter(SessionManagerFactoryConfiguration configuration, C managementConfiguration, Immutability immutability) {
        super(configuration);
        this.maxActiveSessions = configuration.getMaxActiveSessions();
        DeploymentUnit unit = configuration.getDeploymentUnit();
        Module module = unit.getAttachment(Attachments.MODULE);
        this.marshaller = managementConfiguration.getMarshallerFactory().apply(configuration.getDeploymentUnit());
        List<Immutability> loadedImmutabilities = new LinkedList<>();
        for (Immutability loadedImmutability : module.loadService(Immutability.class)) {
            loadedImmutabilities.add(loadedImmutability);
        }
        this.immutability = Immutability.composite(List.of(
                Immutability.getDefault(),
                Immutability.classes(List.of(AuthenticatedSession.class, SavedRequest.class, CachedIdentity.class, IdentityContainer.class)),
                Immutability.composite(loadedImmutabilities),
                immutability));
        this.attributePersistenceStrategy = managementConfiguration.getAttributePersistenceStrategy();
    }

    @Override
    public OptionalInt getMaxSize() {
        return this.maxActiveSessions;
    }

    @Override
    public ByteBufferMarshaller getMarshaller() {
        return this.marshaller;
    }

    @Override
    public Supplier<Map<String, Object>> getSessionContextFactory() {
        return ConcurrentHashMap::new;
    }

    @Override
    public Immutability getImmutability() {
        return this.immutability;
    }

    @Override
    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return this.attributePersistenceStrategy;
    }
}
