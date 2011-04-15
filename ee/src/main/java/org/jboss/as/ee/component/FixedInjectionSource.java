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
import org.jboss.as.naming.StaticManagedObject;
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
     * @param value the fixed value
     */
    public FixedInjectionSource(final Object value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        managedReferenceFactory = new StaticManagedObject(value);
        this.value = value;
    }

    private boolean equals(final InjectionSource injectionSource) {
        return injectionSource instanceof FixedInjectionSource && equals((FixedInjectionSource) injectionSource);
    }

    public boolean equals(final FixedInjectionSource configuration) {
        return configuration != null && value.equals(configuration.value);
    }

    public int hashCode() {
        return FixedInjectionSource.class.hashCode() * 127 + value.hashCode();
    }

    /** {@inheritDoc} */
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        injector.inject(managedReferenceFactory);
    }
}
