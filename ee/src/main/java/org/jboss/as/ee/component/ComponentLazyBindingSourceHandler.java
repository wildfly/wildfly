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

import java.util.Set;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * {@link org.jboss.as.ee.component.LazyBindingSourceDescription.LazyBingingSourceDescriptionHandler} implementation that is capable
 * of determining the value of a resource injection with a binding type that represents a component view.
 *
 * @author John Bailey
 */
public class ComponentLazyBindingSourceHandler implements LazyBindingSourceDescription.LazyBingingSourceDescriptionHandler {

    static ComponentLazyBindingSourceHandler INSTANCE = new ComponentLazyBindingSourceHandler();

    private ComponentLazyBindingSourceHandler() {
    }

    /**
     * Use the available component view information to determine whether this lazy binding can be resolved to a component view.
     *
     * {@inheritDoc}
     */
    public boolean getResourceValue(final BindingDescription referenceDescription, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        final String bindingType = referenceDescription.getBindingType();
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEApplicationDescription applicationComponentDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_DESCRIPTION);
        if (applicationComponentDescription != null) {
            final Set<AbstractComponentDescription> componentDescriptions = applicationComponentDescription.getComponentsForViewName(bindingType);
            if (componentDescriptions != null && !componentDescriptions.isEmpty()) {
                if (componentDescriptions.size() > 1) {
                    throw new IllegalArgumentException("BindingDescription source can not be resolved.  Ambiguous required view [" + bindingType + "]");
                }
                final AbstractComponentDescription targetComponentDescription = componentDescriptions.iterator().next();
                final ServiceName beanServiceName = deploymentUnit.getServiceName()
                        .append("component").append(targetComponentDescription.getComponentName()).append("VIEW").append(bindingType);
                serviceBuilder.addDependency(beanServiceName, ManagedReferenceFactory.class, injector);

                return true;
            }
        }
        return false;  // We were not able to handle this using the available components.
    }
}
