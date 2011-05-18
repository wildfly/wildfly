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
public class EjbBeanNameInjectionSource extends InjectionSource {
    private final String beanName;
    private final String typeName;

    public EjbBeanNameInjectionSource(final String beanName, final String typeName) {
        this.beanName = beanName;
        this.typeName = typeName;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(EE_APPLICATION_DESCRIPTION);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final Set<ViewDescription> componentsForViewName = applicationDescription.getComponents(beanName, typeName, deploymentRoot.getRoot());
        if (componentsForViewName.isEmpty()) {
            throw new DeploymentUnitProcessingException("No component found for type '" + typeName + "' with name " + beanName);
        }
        if (componentsForViewName.size() > 1) {
            throw new DeploymentUnitProcessingException("More than 1 component found for type '" + typeName + "' and bean name " + beanName);
        }
        ViewDescription description = componentsForViewName.iterator().next();
        serviceBuilder.addDependency(description.getServiceName(), ComponentView.class, new ViewManagedReferenceFactory.Injector(injector));

    }

    public boolean equals(final Object injectionSource) {
        return injectionSource instanceof EjbBeanNameInjectionSource && equals((EjbBeanNameInjectionSource) injectionSource);
    }

    private boolean equals(final EjbBeanNameInjectionSource configuration) {
        return configuration != null && typeName.equals(configuration.typeName) && beanName.equals(configuration.beanName);
    }

    public int hashCode() {
        int result = beanName != null ? beanName.hashCode() : 0;
        result = 31 * result + (typeName != null ? typeName.hashCode() : 0);
        return result;
    }
}
