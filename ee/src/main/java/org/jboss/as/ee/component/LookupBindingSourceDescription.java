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
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * A reference to a generalized JNDI resource.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Stuart Douglas
 */
public final class LookupBindingSourceDescription extends BindingSourceDescription {
    private final String lookupName;
    private final ServiceName sourceServiceName;

    public LookupBindingSourceDescription(final String lookupJndiName, AbstractComponentDescription componentDescription) {
        // JBAS-9267 https://issues.jboss.org/browse/JBAS-9267
        // Make sure that the passed lookup jndi name is converted from java:comp to java:module for
        // a component which doesn't have a java:comp of its own
        this.lookupName = this.parseLookupName(lookupJndiName, componentDescription);

        final String compName = componentDescription.getNamingMode() == ComponentNamingMode.CREATE ? componentDescription.getComponentName() : componentDescription.getModuleName();
        final String moduleName = componentDescription.getModuleName();
        final String appName = componentDescription.getApplicationName();
        sourceServiceName = ContextNames.serviceNameOfContext(appName, moduleName, compName, this.lookupName);
    }

    public LookupBindingSourceDescription(final String lookupJndiName, EEModuleDescription moduleDescription) {
        // JBAS-9267 https://issues.jboss.org/browse/JBAS-9267
        // Make sure that the passed lookup jndi name is converted from java:comp to java:module for
        // a component which doesn't have a java:comp of its own
        if (lookupJndiName.startsWith("java:comp/")) {
            String jndiNameSansNamespace = lookupJndiName.substring("java:comp/".length());
            this.lookupName = "java:module/" + jndiNameSansNamespace;
        } else {
            this.lookupName = lookupJndiName;
        }

        final String compName = moduleDescription.getModuleName();
        final String moduleName = moduleDescription.getModuleName();
        final String appName = moduleDescription.getAppName();
        sourceServiceName = ContextNames.serviceNameOfContext(appName, moduleName, compName, this.lookupName);
    }

    public void getResourceValue(final BindingDescription bindingDescription, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        serviceBuilder.addDependency(sourceServiceName, ManagedReferenceFactory.class, injector);
    }

    /**
     * Converts a java:comp/<blah> jndi name to java:module/<blah> jndi name if the passed <code>lookupName</code>
     * starts with java:comp/ namespace and the {@link ComponentNamingMode} of the passed <code>componentDescription</code>
     * is <i>not</i> {@link ComponentNamingMode#CREATE}
     *
     * @param lookupName           The lookup jndi name to be parsed
     * @param componentDescription The component description
     * @return
     */
    private String parseLookupName(String lookupName, AbstractComponentDescription componentDescription) {
        ComponentNamingMode namingMode = componentDescription.getNamingMode();
        if (namingMode != ComponentNamingMode.CREATE) {
            if (lookupName.startsWith("java:comp/")) {
                String jndiNameSansNamespace = lookupName.substring("java:comp/".length());
                return "java:module/" + jndiNameSansNamespace;
            }
        }
        return lookupName;
    }
}
