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

package org.wildfly.clustering.web.undertow.session;

import io.undertow.servlet.api.SessionManagerFactory;

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * {@link org.jboss.msc.Service} that returns a distributable {@link SessionManagerFactory}.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactoryService extends AbstractService<SessionManagerFactory> {

    public static ServiceBuilder<SessionManagerFactory> build(ServiceTarget target, ServiceName name, ServiceName factoryServiceName) {
        DistributableSessionManagerFactoryService service = new DistributableSessionManagerFactoryService();
        return target.addService(name, service)
                .addDependency(factoryServiceName, org.wildfly.clustering.web.session.SessionManagerFactory.class, service.factory)
        ;
    }

    @SuppressWarnings("rawtypes")
    private final InjectedValue<org.wildfly.clustering.web.session.SessionManagerFactory> factory = new InjectedValue<>();

    @Override
    public SessionManagerFactory getValue() {
        return new DistributableSessionManagerFactory(this.factory.getValue());
    }
}
