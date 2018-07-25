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

import org.jboss.modules.Module;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.undertow.session.DistributableSessionManagerFactoryServiceConfigurator.MarshallingVersion;

/**
 * @author Paul Ferraro
 */
public class SessionManagerFactoryConfigurationAdapter extends WebDeploymentConfigurationAdapter implements org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration<MarshallingContext, LocalSessionContext> {

    private final Integer maxActiveSessions;
    private final MarshallingContext context;
    private final MarshalledValueFactory<MarshallingContext> marshalledValueFactory;
    private final LocalContextFactory<LocalSessionContext> localContextFactory = new LocalSessionContextFactory();

    public SessionManagerFactoryConfigurationAdapter(SessionManagerFactoryConfiguration configuration) {
        super(configuration);
        this.maxActiveSessions = configuration.getMaxActiveSessions();
        Module module = configuration.getModule();
        this.context = new SimpleMarshallingContextFactory().createMarshallingContext(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, module), module.getClassLoader());
        this.marshalledValueFactory = new SimpleMarshalledValueFactory(this.context);
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
    public MarshallingContext getMarshallingContext() {
        return this.context;
    }

    @Override
    public LocalContextFactory<LocalSessionContext> getLocalContextFactory() {
        return this.localContextFactory;
    }
}
