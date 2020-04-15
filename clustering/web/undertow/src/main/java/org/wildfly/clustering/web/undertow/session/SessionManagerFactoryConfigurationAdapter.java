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
import java.util.ServiceLoader;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingListener;

import org.jboss.modules.Module;
import org.wildfly.clustering.ee.CompositeIterable;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.immutable.CompositeImmutability;
import org.wildfly.clustering.ee.immutable.DefaultImmutability;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.SpecificationProvider;
import org.wildfly.clustering.web.session.SessionAttributeImmutability;
import org.wildfly.clustering.web.undertow.session.DistributableSessionManagerFactoryServiceConfigurator.MarshallingVersion;

/**
 * @author Paul Ferraro
 */
public class SessionManagerFactoryConfigurationAdapter extends WebDeploymentConfigurationAdapter implements org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration<HttpSession, ServletContext, HttpSessionActivationListener, HttpSessionBindingListener, MarshallingContext, LocalSessionContext> {

    private final Integer maxActiveSessions;
    private final MarshallingContext context;
    private final MarshalledValueFactory<MarshallingContext> marshalledValueFactory;
    private final LocalContextFactory<LocalSessionContext> localContextFactory = new LocalSessionContextFactory();
    private final Immutability immutability;

    public SessionManagerFactoryConfigurationAdapter(SessionManagerFactoryConfiguration configuration, Immutability immutability) {
        super(configuration);
        this.maxActiveSessions = configuration.getMaxActiveSessions();
        Module module = configuration.getModule();
        this.context = new SimpleMarshallingContextFactory().createMarshallingContext(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, module), module.getClassLoader());
        this.marshalledValueFactory = new SimpleMarshalledValueFactory(this.context);
        ServiceLoader<Immutability> loadedImmutability = ServiceLoader.load(Immutability.class, Immutability.class.getClassLoader());
        this.immutability = new CompositeImmutability(new CompositeIterable<>(EnumSet.allOf(DefaultImmutability.class), EnumSet.allOf(SessionAttributeImmutability.class), EnumSet.allOf(UndertowSessionAttributeImmutability.class), loadedImmutability, Collections.singleton(immutability)));
    }

    @Override
    public Integer getMaxActiveSessions() {
        return this.maxActiveSessions;
    }

    @Override
    public MarshalledValueFactory<MarshallingContext> getMarshalledValueFactory() {
        return this.marshalledValueFactory;
    }

    @Override
    public LocalContextFactory<LocalSessionContext> getLocalContextFactory() {
        return this.localContextFactory;
    }

    @Override
    public Immutability getImmutability() {
        return this.immutability;
    }

    @Override
    public SpecificationProvider<HttpSession, ServletContext, HttpSessionActivationListener, HttpSessionBindingListener> getSpecificationProvider() {
        return UndertowSpecificationProvider.INSTANCE;
    }
}
