/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import io.undertow.servlet.api.SessionManagerFactory;
import io.undertow.servlet.core.InMemorySessionManagerFactory;

import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactoryBuilderProvider;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.session.DistributableSessionManagerConfiguration;

/**
 * Distributable {@link SessionManagerFactory} builder for Undertow.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactoryBuilder implements org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilder {

    static final Map<ReplicationGranularity, SessionManagerFactoryConfiguration.SessionAttributePersistenceStrategy> strategies = new EnumMap<>(ReplicationGranularity.class);
    static {
        strategies.put(ReplicationGranularity.SESSION, SessionManagerFactoryConfiguration.SessionAttributePersistenceStrategy.COARSE);
        strategies.put(ReplicationGranularity.ATTRIBUTE, SessionManagerFactoryConfiguration.SessionAttributePersistenceStrategy.FINE);
    }

    @SuppressWarnings("rawtypes")
    private static final Optional<SessionManagerFactoryBuilderProvider> PROVIDER = StreamSupport.stream(ServiceLoader.load(SessionManagerFactoryBuilderProvider.class, SessionManagerFactoryBuilderProvider.class.getClassLoader()).spliterator(), false).findFirst();

    @Override
    public ServiceBuilder<SessionManagerFactory> build(ServiceTarget target, ServiceName name, final DistributableSessionManagerConfiguration config) {
        if (!PROVIDER.isPresent()) {
            UndertowLogger.ROOT_LOGGER.clusteringNotSupported();
            // Fallback to local session manager if no provider exists
            SessionManagerFactory factory = new InMemorySessionManagerFactory(config.getMaxActiveSessions());
            return target.addService(name,  new ValueService<>(new ImmediateValue<>(factory))).setInitialMode(Mode.ON_DEMAND);
        }

        SessionManagerFactoryConfiguration configuration = new SessionManagerFactoryConfiguration() {
            @Override
            public int getMaxActiveSessions() {
                return config.getMaxActiveSessions();
            }

            @Override
            public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
                return strategies.get(config.getGranularity());
            }

            @Override
            public String getServerName() {
                return config.getServerName();
            }

            @Override
            public String getDeploymentName() {
                return config.getDeploymentName();
            }

            @Override
            public Module getModule() {
                return config.getModule();
            }

            @Override
            public String getCacheName() {
                return config.getCacheName();
            }
        };
        SessionManagerFactoryBuilderProvider<Batch> provider = PROVIDER.get();
        Builder<org.wildfly.clustering.web.session.SessionManagerFactory<Batch>> builder = provider.getBuilder(configuration);
        builder.build(target).install();
        @SuppressWarnings("rawtypes")
        InjectedValue<org.wildfly.clustering.web.session.SessionManagerFactory> factory = new InjectedValue<>();
        Value<SessionManagerFactory> value = () -> new DistributableSessionManagerFactory(factory.getValue());
        return target.addService(name, new ValueService<>(value))
                .addDependency(builder.getServiceName(), org.wildfly.clustering.web.session.SessionManagerFactory.class, factory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
