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
package org.wildfly.clustering.server.group;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Builder;

/**
 * Builds a non-clustered {@link JGroupsNodeFactory} service.
 * @author Paul Ferraro
 */
public class LocalNodeFactoryBuilder extends GroupNodeFactoryServiceNameProvider implements Builder<JGroupsNodeFactory>, Value<JGroupsNodeFactory> {

    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<>();

    public LocalNodeFactoryBuilder(String group) {
        super(group);
    }

    @Override
    public ServiceBuilder<JGroupsNodeFactory> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, this.environment)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public JGroupsNodeFactory getValue() {
        return new LocalNodeFactory(this.group, this.environment.getValue().getNodeName());
    }
}
