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
package org.jboss.as.ee.concurrent.deployers;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ServiceInjectionSource;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ManagedThreadFactoryService;

import java.util.List;

/**
 * The {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} which sets up the default concurrent managed thread factory for each EE component in the deployment unit.
 *
 * @author Eduardo Martins
 */
public class EEConcurrentDefaultManagedThreadFactoryProcessor extends EEConcurrentDefaultAbstractProcessor {

    @Override
    void addBindingsConfigurations(String bindingNamePrefix, List<BindingConfiguration> bindingConfigurations) {
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultManagedThreadFactory", new ServiceInjectionSource(ConcurrentServiceNames.DEFAULT_MANAGED_THREAD_FACTORY_SERVICE_NAME, ManagedThreadFactoryService.SERVICE_VALUE_TYPE)));
    }

}
