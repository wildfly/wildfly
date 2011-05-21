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

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.ViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

import java.util.Set;

import static org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION;

/**
 * Implementation of {@link InjectionSource} responsible for finding a specific bean instance with a bean name and interface.
 *
 * @author John Bailey
 */
public class EjbInjectionSource extends InjectionSource {
    private final String beanName;
    private final String typeName;

    public EjbInjectionSource(final String beanName, final String typeName) {
        this.beanName = beanName;
        this.typeName = typeName;
    }

    public EjbInjectionSource(final String typeName) {
        this.beanName = null;
        this.typeName = typeName;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final Set<ViewDescription> componentsForViewName = getViews(phaseContext);
        if (componentsForViewName.isEmpty()) {
            throw new DeploymentUnitProcessingException("No component found for type '" + typeName + "' with name " + beanName);
        }
        if (componentsForViewName.size() > 1) {
            throw new DeploymentUnitProcessingException("More than 1 component found for type '" + typeName + "' and bean name " + beanName);
        }
        ViewDescription description = componentsForViewName.iterator().next();
        serviceBuilder.addDependency(description.getServiceName(), ComponentView.class, new ViewManagedReferenceFactory.Injector(injector));

    }

    private Set<ViewDescription> getViews(final DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(EE_APPLICATION_DESCRIPTION);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final Set<ViewDescription> componentsForViewName;
        if (beanName != null) {
            componentsForViewName = applicationDescription.getComponents(beanName, typeName, deploymentRoot.getRoot());
        } else {
            componentsForViewName = applicationDescription.getComponentsForViewName(typeName);
        }
        return componentsForViewName;
    }

    @Override
    public boolean equalTo(final InjectionSource injectionSource, final DeploymentPhaseContext phaseContext) {
        return injectionSource instanceof EjbInjectionSource && equals((EjbInjectionSource) injectionSource, phaseContext);
    }

    private boolean equals(final EjbInjectionSource configuration, final DeploymentPhaseContext phaseContext) {
        if(this == configuration) {
            return true;
        }
        final Set<ViewDescription> theseViews = getViews(phaseContext);
        final Set<ViewDescription> otherViews = configuration.getViews(phaseContext);
        //return true if they resolve to the same set of views
        //it does not matter if the resolution is correct at this point
        //as an error will occur later
        return otherViews.equals(theseViews);
    }

}
