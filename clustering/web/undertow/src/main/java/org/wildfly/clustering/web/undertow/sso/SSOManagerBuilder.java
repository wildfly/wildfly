/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow.sso;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.undertow.IdentifierFactoryAdapter;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.server.session.SessionIdGenerator;

/**
 * @author Paul Ferraro
 */
public class SSOManagerBuilder implements Builder<SSOManager<AuthenticatedSession, String, Void, Batch>>, Service<SSOManager<AuthenticatedSession, String, Void, Batch>>, LocalContextFactory<Void> {

    private final InjectedValue<SessionIdGenerator> generator = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<SSOManagerFactory> factory = new InjectedValue<>();
    private final ServiceName factoryServiceName;
    private final ServiceName generatorServiceName;

    private volatile SSOManager<AuthenticatedSession, String, Void, Batch> manager;

    public SSOManagerBuilder(ServiceName factoryServiceName, ServiceName generatorServiceName) {
        this.factoryServiceName = factoryServiceName;
        this.generatorServiceName = generatorServiceName;
    }

    @Override
    public ServiceName getServiceName() {
        return this.factoryServiceName.append("manager");
    }

    @Override
    public ServiceBuilder<SSOManager<AuthenticatedSession, String, Void, Batch>> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), this)
                .addDependency(this.factoryServiceName, SSOManagerFactory.class, this.factory)
                .addDependency(this.generatorServiceName, SessionIdGenerator.class, this.generator)
                ;
    }

    @Override
    public void start(StartContext context) throws StartException {
        SSOManagerFactory<AuthenticatedSession, String, Batch> factory = this.factory.getValue();
        this.manager = factory.createSSOManager(new IdentifierFactoryAdapter(this.generator.getValue()), this);
        this.manager.start();
    }

    @Override
    public void stop(StopContext context) {
        this.manager.stop();
        this.manager = null;
    }

    @Override
    public SSOManager<AuthenticatedSession, String, Void, Batch> getValue() {
        return this.manager;
    }

    @Override
    public Void createLocalContext() {
        return null;
    }
}
