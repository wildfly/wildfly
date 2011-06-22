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
import org.jboss.msc.service.ServiceName;

import java.util.Set;

import static org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION;

/**
 * Implementation of {@link InjectionSource} responsible for finding a specific bean instance with a bean name and interface.
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public class EjbInjectionSource extends InjectionSource {
    private final String beanName;
    private final String typeName;
    private volatile ServiceName resolvedViewName;
    private volatile String error = null;

    public EjbInjectionSource(final String beanName, final String typeName) {
        this.beanName = beanName;
        this.typeName = typeName;
    }

    public EjbInjectionSource(final String typeName) {
        this.beanName = null;
        this.typeName = typeName;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        if(error != null) {
            throw new DeploymentUnitProcessingException(error);
        }
        serviceBuilder.addDependency(resolvedViewName, ComponentView.class, new ViewManagedReferenceFactory.Injector(injector));

    }

    public void resolve(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final Set<ViewDescription> componentsForViewName = getViews(phaseContext);
        //we cannot be sure that this injection will actually be used, so we wait until getResourceValue is called to throw an exception
        if (componentsForViewName.isEmpty()) {
            error = "No component found for type '" + typeName + "' with name " + beanName;
            return;
        }
        if (componentsForViewName.size() > 1) {
            error = "More than 1 component found for type '" + typeName + "' and bean name " + beanName;
            return;
        }
        ViewDescription description = componentsForViewName.iterator().next();
        ServiceName serviceName = description.getServiceName();
        resolvedViewName = serviceName;
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

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof EjbInjectionSource))
            return false;
        if(error != null) {
            //we can't do a real equals comparison in this case, so throw the original error
            throw new RuntimeException(error);
        }
        if(resolvedViewName == null) {
            throw new RuntimeException("Error equals() cannot be called before resolve()");
        }

        EjbInjectionSource other = (EjbInjectionSource) o;
        return eq(typeName, other.typeName) && eq(resolvedViewName, other.resolvedViewName);
    }

    public int hashCode() {
        return typeName.hashCode();
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
