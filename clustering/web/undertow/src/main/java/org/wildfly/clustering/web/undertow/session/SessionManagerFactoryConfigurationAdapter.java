/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow.session;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.ServiceLoader;

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
import org.wildfly.clustering.web.LocalContextFactory;
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
        ServiceLoader<Immutability> loadedImmutability = module.loadService(Immutability.class);
        this.immutability = new CompositeImmutability(new CompositeIterable<>(EnumSet.allOf(DefaultImmutability.class), EnumSet.allOf(SessionAttributeImmutability.class), EnumSet.allOf(UndertowSessionAttributeImmutability.class), loadedImmutability, Collections.singleton(immutability)));
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
    public LocalContextFactory<Map<String, Object>> getLocalContextFactory() {
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
