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

package org.wildfly.extension.clustering.server;

import java.util.List;
import java.util.function.Function;

import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.server.service.IdentityGroupServiceConfiguratorProvider;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class IdentityGroupRequirementServiceConfiguratorProvider implements IdentityGroupServiceConfiguratorProvider {

    private final ClusteringRequirement requirement;
    private final Function<String, JndiName> jndiNameFactory;

    protected IdentityGroupRequirementServiceConfiguratorProvider(ClusteringRequirement requirement) {
        this(requirement, null);
    }

    protected IdentityGroupRequirementServiceConfiguratorProvider(ClusteringRequirement requirement, Function<String, JndiName> jndiNameFactory) {
        this.requirement = requirement;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String group, String targetGroup) {
        ServiceName name = this.requirement.getServiceName(support, group);
        ServiceName targetName = this.requirement.getServiceName(support, targetGroup);
        ServiceConfigurator configurator = new IdentityServiceConfigurator<>(name, targetName);
        if ((this.jndiNameFactory == null) || JndiNameFactory.DEFAULT_LOCAL_NAME.equals(targetGroup)) {
            return List.of(configurator);
        }
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(group).getAbsoluteName());
        ServiceConfigurator binderConfigurator = new BinderServiceConfigurator(binding, configurator.getServiceName()).configure(support);
        return List.of(configurator, binderConfigurator);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
