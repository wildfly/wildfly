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
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

import java.util.List;

/**
 * {@link BindingSourceDescription} that will lazily determine the resource value as new mechanisms for determining resouce values
 * are added with the addition of additional {@link LazyBingingSourceDescriptionHandler} instances.
 *
 * @author John Bailey
 */
public class LazyBindingSourceDescription extends BindingSourceDescription {
    /**
     * {@inheritDoc}
     */
    public void getResourceValue(final BindingDescription referenceDescription, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        final List<LazyBingingSourceDescriptionHandler> handlers = phaseContext.getAttachment(Attachments.LAZY_BINDING_SOURCES_HANDLERS);
        if (handlers != null) for (LazyBingingSourceDescriptionHandler handler : handlers) {
            if (handler.getResourceValue(referenceDescription, serviceBuilder, phaseContext, injector)) {
                return;
            }
        }
        // None if the handlers were able to resolve the binding source
        throw new IllegalArgumentException("Failed to determine resource binding value for " + referenceDescription);
    }

    /**
     * Handler capable of determining the value for a {@link BindingSourceDescription} that set for lazy determination.
     */
    public static interface LazyBingingSourceDescriptionHandler {
        /**
         * Get the resouce value for a {@link LazyBindingSourceDescription}.  Implementations of this method should return the
         * correct boolean value determining whether or not they handled the binding source.
         *
         *
         * @param referenceDescription the reference description describing the reference
         * @param serviceBuilder       the service builder to append dependencies to, if any
         * @param phaseContext         the deployment phase context
         * @param injector             the injector into which the value should be placed
         * @return true if the handler was able to provide a value, false otherwise.
         */
        boolean getResourceValue(BindingDescription referenceDescription, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector);
    }
}
