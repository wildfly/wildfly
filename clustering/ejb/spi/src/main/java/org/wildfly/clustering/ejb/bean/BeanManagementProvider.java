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
package org.wildfly.clustering.ejb.bean;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;

/**
 * Provides service installation mechanics for components of bean deployments.
 * @author Paul Ferraro
 */
public interface BeanManagementProvider {

    /**
     * Returns a name uniquely identifying this provider.
     * @return the provider name
     */
    String getName();

    /**
     * Installs dependencies for a deployment unit
     * @param configuration a bean deployment configuration
     * @return a collection of service configurators
     */
    Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(BeanDeploymentConfiguration configuration);

    /**
     * Builds a bean manager factory for an Jakarta Enterprise Bean within a deployment.
     * @param configuration a bean configuration
     * @return a service configurator
     */
    CapabilityServiceConfigurator getBeanManagerFactoryServiceConfigurator(BeanConfiguration configuration);
}
