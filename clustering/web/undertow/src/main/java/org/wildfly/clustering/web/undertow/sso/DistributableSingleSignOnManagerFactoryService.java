/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.extension.undertow.security.sso.SingleSignOnManagerFactory;

/**
 * Service that provides a distributable {@link SingleSignOnManagerFactory}.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerFactoryService extends AbstractService<SingleSignOnManagerFactory> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static ServiceBuilder<SingleSignOnManagerFactory> build(ServiceTarget target, ServiceName name, ServiceName hostServiceName, ServiceName managerServiceName) {
        DistributableSingleSignOnManagerFactoryService service = new DistributableSingleSignOnManagerFactoryService();
        return target.addService(name, service)
                .addDependency(managerServiceName, SSOManagerFactory.class, (Injector) service.manager)
                .addDependency(SessionManagerRegistryService.getServiceName(hostServiceName), SessionManagerRegistry.class, service.registry)
        ;
    }

    private final InjectedValue<SSOManagerFactory<AuthenticatedSession, String>> manager = new InjectedValue<>();
    private final InjectedValue<SessionManagerRegistry> registry = new InjectedValue<>();

    private DistributableSingleSignOnManagerFactoryService() {
        // Hide
    }

    @Override
    public SingleSignOnManagerFactory getValue() {
        return new DistributableSingleSignOnManagerFactory(this.manager.getValue(), this.registry.getValue());
    }
}
