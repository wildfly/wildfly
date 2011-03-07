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

/**
 * A description of a source for a JNDI binding.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class BindingSourceDescription {

    /**
     * Construct a new instance.
     */
    protected BindingSourceDescription() {
    }

    /**
     * Get the value to use as the injection source.  The value will be yield an injectable which is dereferenced once
     * for every time the reference source is injected.
     *
     * @param componentDescription the component upon whose behalf this reference value is being acquired
     * @param referenceDescription the reference description describing the reference
     * @param serviceBuilder the service builder to append dependencies to, if any
     * @param phaseContext the deployment phase context
     * @param injector the injector into which the value should be placed
     */
    public abstract void getResourceValue(AbstractComponentDescription componentDescription, BindingDescription referenceDescription, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector);
}
