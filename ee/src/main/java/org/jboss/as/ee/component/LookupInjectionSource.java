/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * A binding which gets its value from another JNDI binding.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LookupInjectionSource extends InjectionSource {
    private final String lookupName;

    public LookupInjectionSource(final String lookupName) {
        if (lookupName == null) {
            throw new IllegalArgumentException("lookupName is null");
        }
        this.lookupName = lookupName;
    }

    /**
     * {@inheritDoc}
     */
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        final String applicationName = resolutionContext.getApplicationName();
        final String moduleName = resolutionContext.getModuleName();
        final String componentName = resolutionContext.getComponentName();
        final boolean compUsesModule = resolutionContext.isCompUsesModule();
        final String lookupName;
        if (!this.lookupName.startsWith("java:")) {
            if (componentName != null && !compUsesModule) {
                lookupName = "java:comp/env/" + this.lookupName;
            } else if (compUsesModule) {
                lookupName = "java:module/env/" + this.lookupName;
            } else {
                lookupName = "java:jboss/env" + this.lookupName;
            }
        } else if (this.lookupName.startsWith("java:comp/") && compUsesModule) {
            lookupName = "java:module/" + this.lookupName.substring(10);
        } else {
            lookupName = this.lookupName;
        }
        final ServiceName serviceName = ContextNames.serviceNameOfContext(applicationName, moduleName, componentName, lookupName);
        serviceBuilder.addDependency(serviceName, ManagedReferenceFactory.class, injector);
    }


    public boolean equals(Object configuration) {
        if (configuration instanceof LookupInjectionSource) {
            LookupInjectionSource lookup = (LookupInjectionSource) configuration;
            return lookupName.equals(lookup.lookupName);
        }
        return false;
    }

    public int hashCode() {
        return lookupName.hashCode();
    }

}
