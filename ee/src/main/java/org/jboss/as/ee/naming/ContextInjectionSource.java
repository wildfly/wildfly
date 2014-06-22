/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.naming;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ContextManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 *
 * Injection source that can be used to bind a potentially empty context
 *
* @author Stuart Douglas
*/
public class ContextInjectionSource extends InjectionSource {

    private final String name;
    private final String fullName;

    public ContextInjectionSource(final String name, String fullName) {
        this.name = name;
        this.fullName = fullName;
    }

    @Override
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final ContextManagedReferenceFactory managedReferenceFactory = new ContextManagedReferenceFactory(name);
        final ServiceName contextServiceName;
        if(fullName.startsWith("java:app")) {
            contextServiceName = ContextNames.contextServiceNameOfApplication(resolutionContext.getApplicationName());
        } else if (fullName.startsWith("java:module") || (fullName.startsWith("java:comp") && resolutionContext.isCompUsesModule())) {
            contextServiceName = ContextNames.contextServiceNameOfModule(resolutionContext.getApplicationName(), resolutionContext.getModuleName());
        } else if(fullName.startsWith("java:comp")) {
            contextServiceName = ContextNames.contextServiceNameOfComponent(resolutionContext.getApplicationName(), resolutionContext.getModuleName(), resolutionContext.getComponentName());
        } else {
            throw NamingLogger.ROOT_LOGGER.invalidNameForContextBinding(fullName);
        }
        serviceBuilder.addDependency(contextServiceName, NamingStore.class, managedReferenceFactory.getNamingStoreInjectedValue());
        injector.inject(managedReferenceFactory);
    }
}
