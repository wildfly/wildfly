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
package org.jboss.as.ejb3.cache;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;

/**
 * Builds a {@link CacheFactory} service.
 *
 * @author Paul Ferraro
 *
 * @param <K> the cache key
 * @param <V> the cache value
 */
public interface CacheFactoryBuilder<K, V extends Identifiable<K>> {
    /**
     * Returns configurators for services to be installed for the specified deployment.
     * @param unit a deployment unit
     * @return a collection of service configurators
     */
    Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(DeploymentUnit unit);

    /**
     * Returns a configurator for a service supplying a cache factory.
     * @param name the service name of the cache factory
     * @param description the component description
     * @param configuration the component configuration
     * @return a service configurator
     */
    CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, StatefulComponentDescription description, ComponentConfiguration configuration);

    /**
     * Indicates whether or not cache factories built by this object can support passivation.
     * @return true, if passivation is supported, false otherwise.
     */
    boolean supportsPassivation();
}
