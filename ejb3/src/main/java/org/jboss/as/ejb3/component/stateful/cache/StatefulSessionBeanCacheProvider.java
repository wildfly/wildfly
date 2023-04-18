/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateful.cache;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Provides configurators for services to install a stateful session bean cache factory.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface StatefulSessionBeanCacheProvider<K, V extends StatefulSessionBeanInstance<K>> {
    /**
     * Returns configurators for services to be installed for the specified deployment.
     * @param unit a deployment unit
     * @return a collection of service configurators
     */
    Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(DeploymentUnit unit, EEModuleConfiguration moduleConfiguration);

    /**
     * Returns a configurator for a service supplying a cache factory.
     * @param unit the deployment unit containing this EJB component.
     * @param description the EJB component description
     * @param configuration the component configuration
     * @return a service configurator
     */
    CapabilityServiceConfigurator getStatefulBeanCacheFactoryServiceConfigurator(DeploymentUnit unit, StatefulComponentDescription description, ComponentConfiguration configuration);

    /**
     * Indicates whether or not cache factories provides by this object can support passivation.
     * @return true, if passivation is supported, false otherwise.
     */
    boolean supportsPassivation();
}
