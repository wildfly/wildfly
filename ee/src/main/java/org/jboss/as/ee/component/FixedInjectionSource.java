/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
