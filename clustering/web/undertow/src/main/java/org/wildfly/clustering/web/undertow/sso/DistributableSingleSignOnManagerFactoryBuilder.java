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

import java.util.ServiceLoader;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilder;
import org.wildfly.extension.undertow.security.sso.SingleSignOnManagerFactory;


/**
 * Builds a distributable {@link SingleSignOnManagerFactory} service.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerFactoryBuilder implements org.wildfly.extension.undertow.security.sso.DistributableSingleSignOnManagerFactoryBuilder {

    private static SSOManagerFactoryBuilder load() {
        for (SSOManagerFactoryBuilder builder: ServiceLoader.load(SSOManagerFactoryBuilder.class, SSOManagerFactoryBuilder.class.getClassLoader())) {
            return builder;
        }
        return null;
    }

    private final SSOManagerFactoryBuilder builder;

    public DistributableSingleSignOnManagerFactoryBuilder() {
        this(load());
    }

    private DistributableSingleSignOnManagerFactoryBuilder(SSOManagerFactoryBuilder builder) {
        this.builder = builder;
    }

    @Override
    public ServiceBuilder<SingleSignOnManagerFactory> build(ServiceTarget target, ServiceName name, final ServiceName hostServiceName) {
        ServiceName managerServiceName = name.append("clustering");
        this.builder.build(target, managerServiceName, hostServiceName.getSimpleName()).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        SessionManagerRegistryService.build(target, hostServiceName).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        return DistributableSingleSignOnManagerFactoryService.build(target, name, hostServiceName, managerServiceName);
    }
}
