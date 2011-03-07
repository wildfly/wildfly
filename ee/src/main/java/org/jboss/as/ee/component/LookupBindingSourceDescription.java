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

import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.naming.JndiInjectable;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * A reference to a generalized JNDI resource.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LookupBindingSourceDescription extends BindingSourceDescription {
    private final String lookupName;

    public LookupBindingSourceDescription(final String lookupName) {
        this.lookupName = lookupName;
    }

    public void getResourceValue(final AbstractComponentDescription componentDescription, final BindingDescription bindingDescription, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<JndiInjectable> injector) {
        if (lookupName.startsWith("java:")) {
            // Comes from a scoped dependency item
            final String compName = componentDescription.getNamingMode() == ComponentNamingMode.CREATE ? componentDescription.getComponentName() : componentDescription.getModuleName();
            final String moduleName = componentDescription.getModuleName();
            final String appName = componentDescription.getApplicationName();
            final ServiceName sourceServiceName = ContextNames.serviceNameOfContext(appName, moduleName, compName, lookupName);
            serviceBuilder.addDependency(sourceServiceName, JndiInjectable.class, injector);
        }else {
            throw new RuntimeException("lookup name " + lookupName + " not supported, lookup in @Resource annotation must start with java in " + componentDescription.getComponentClassName());
        }
    }
}
