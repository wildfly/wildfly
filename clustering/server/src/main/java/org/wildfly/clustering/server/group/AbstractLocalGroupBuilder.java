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

package org.wildfly.clustering.server.group;

import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.MappedValueService;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builds a non-clustered {@link Group}.
 * @author Paul Ferraro
 */
public abstract class AbstractLocalGroupBuilder implements CapabilityServiceBuilder<Group>, Function<ServerEnvironment, Group> {

    private final ServiceName name;
    private final ValueDependency<ServerEnvironment> environment = new InjectedValueDependency<>(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class);

    public AbstractLocalGroupBuilder(ServiceName name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public ServiceBuilder<Group> build(ServiceTarget target) {
        return this.environment.register(target.addService(this.name, new MappedValueService<>(this, this.environment))).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
