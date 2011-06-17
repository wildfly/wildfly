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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.EEModuleConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.ejb3.context.CurrentEJBContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * Deployment processor responsible for detecting EJB components and adding a {@link BindingConfiguration} for the
 * java:comp/EJBContext entry.
 *
 * @author John Bailey
 */
public class EjbContextJndiBindingProcessor extends AbstractComponentConfigProcessor {
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        if (!(componentDescription instanceof EJBComponentDescription)) {
            return;  // Only process EJBs
        }
        // if the EJB is packaged in a .war, then we need to bind the java:comp/EJBContext only once for the entire module
        if (componentDescription.getNamingMode() != ComponentNamingMode.CREATE) {
            // get the module description
            final EEModuleDescription moduleDescription = componentDescription.getModuleDescription();
            // create a configurator which binds at the module level
            moduleDescription.getConfigurators().add(new EEModuleConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, EEModuleDescription description, EEModuleConfiguration configuration) throws DeploymentUnitProcessingException {
                    // the java:module/EJBContext binding configuration
                    // Note that we bind to java:module/EJBContext since it's a .war. End users can still lookup java:comp/EJBContext
                    // and that will internally get translated to  java:module/EJBContext for .war, since java:comp == java:module in
                    // a web ENC. So binding to java:module/EJBContext is OK.
                    final BindingConfiguration ejbContextBinding = new BindingConfiguration("java:module/EJBContext", directEjbContextReferenceSource);
                    configuration.getBindingConfigurations().add(ejbContextBinding);
                }
            });
        } else { // EJB packaged outside of a .war. So process normally.
            // add the binding configuration to the component description
            final BindingConfiguration ejbContextBinding = new BindingConfiguration("java:comp/EJBContext", directEjbContextReferenceSource);
            componentDescription.getBindingConfigurations().add(ejbContextBinding);
        }
    }

    private static final ManagedReference ejbContextManagedReference = new ManagedReference() {
        public void release() {
        }

        public Object getInstance() {
            return CurrentEJBContext.get();
        }
    };

    private static final ManagedReferenceFactory ejbContextManagedReferenceFactory = new ManagedReferenceFactory() {
        public ManagedReference getReference() {
            return ejbContextManagedReference;
        }
    };

    private static final InjectionSource directEjbContextReferenceSource = new InjectionSource() {
        public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
            injector.inject(ejbContextManagedReferenceFactory);
        }

        public boolean equals(Object o) {
            return o != null && o.getClass() == this.getClass();
        }

        public int hashCode() {
            return 1;
        }
    };
}
