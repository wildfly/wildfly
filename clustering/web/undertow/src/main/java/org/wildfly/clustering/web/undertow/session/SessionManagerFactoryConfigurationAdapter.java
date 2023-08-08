/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.immutable.CompositeImmutability;
import org.wildfly.clustering.ee.immutable.DefaultImmutability;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.session.SessionAttributeImmutability;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.session.SpecificationProvider;
import org.wildfly.common.iteration.CompositeIterable;

/**
 * @author Paul Ferraro
 */
public class SessionManagerFactoryConfigurationAdapter<C extends DistributableSessionManagementConfiguration<DeploymentUnit>> extends WebDeploymentConfigurationAdapter implements org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, Map<String, Object>> {

    private final Integer maxActiveSessions;
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
        this.immutability = new CompositeImmutability(new CompositeIterable<>(EnumSet.allOf(DefaultImmutability.class), EnumSet.allOf(SessionAttributeImmutability.class), EnumSet.allOf(UndertowSessionAttributeImmutability.class), loadedImmutabilities, List.of(immutability)));
        this.attributePersistenceStrategy = managementConfiguration.getAttributePersistenceStrategy();
    }

    @Override
    public Integer getMaxActiveSessions() {
        return this.maxActiveSessions;
    }

    @Override
    public ByteBufferMarshaller getMarshaller() {
        return this.marshaller;
    }

    @Override
    public Supplier<Map<String, Object>> getLocalContextFactory() {
        return LocalSessionContextFactory.INSTANCE;
    }

    @Override
    public Immutability getImmutability() {
        return this.immutability;
    }

    @Override
    public SpecificationProvider<HttpSession, ServletContext, HttpSessionActivationListener> getSpecificationProvider() {
        return UndertowSpecificationProvider.INSTANCE;
    }

    @Override
    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return this.attributePersistenceStrategy;
    }
}
