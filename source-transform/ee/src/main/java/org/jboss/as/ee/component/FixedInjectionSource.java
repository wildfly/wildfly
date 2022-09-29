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
 * An injection of a fixed value.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class FixedInjectionSource extends InjectionSource {
    private final Object value;
    private final ManagedReferenceFactory managedReferenceFactory;

    /**
     * Construct a new instance.
     *
     * @param factory The managed reference factory to inject
     * @param value the value to use for equality comparison
     */
    public FixedInjectionSource(final ManagedReferenceFactory factory, final Object value) {
        managedReferenceFactory = factory;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        injector.inject(managedReferenceFactory);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FixedInjectionSource == false) {
            return false;
        }
        FixedInjectionSource other = (FixedInjectionSource) obj;
        return this.equalTo(other);
    }

    private boolean equalTo(final FixedInjectionSource configuration) {
        return configuration != null && value.equals(configuration.value);
    }

    public int hashCode() {
        return value.hashCode();
    }
}
