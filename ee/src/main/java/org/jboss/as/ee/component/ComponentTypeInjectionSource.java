/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import java.util.Iterator;
import java.util.Set;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

import static org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION;

/**
 * An injection source which injects a component based upon its type.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ComponentTypeInjectionSource extends InjectionSource {
    private final String typeName;

    public ComponentTypeInjectionSource(final String typeName) {
        this.typeName = typeName;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(EE_APPLICATION_DESCRIPTION);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        final Set<ViewDescription> componentsForViewName = applicationDescription.getComponentsForViewName(typeName, deploymentRoot.getRoot());
        final Iterator<ViewDescription> iterator = componentsForViewName.iterator();
        if (!iterator.hasNext()) {
            throw EeLogger.ROOT_LOGGER.componentNotFound(typeName);
        }
        final ViewDescription description = iterator.next();
        if (iterator.hasNext()) {
            throw EeLogger.ROOT_LOGGER.multipleComponentsFound(typeName);
        }

        //TODO: should ComponentView also be a managed reference factory?
        serviceBuilder.addDependency(description.getServiceName(), ComponentView.class, new ViewManagedReferenceFactory.Injector(injector));
    }

    public boolean equals(final Object other) {
        if (other instanceof ComponentTypeInjectionSource) {
            return ((ComponentTypeInjectionSource) other).typeName.equals(typeName);
        }
        return false;
    }

    public int hashCode() {
        return typeName.hashCode();
    }
}
