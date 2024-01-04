/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * A description of an env-entry.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EnvEntryInjectionSource extends InjectionSource {
    private final Object value;

    /**
     * Construct a new instance.
     *
     * @param value the immediate value of the env entry.
     */
    public EnvEntryInjectionSource(final Object value) {
        this.value = value;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        injector.inject(new ValueManagedReferenceFactory(value));
    }

    public boolean equals(final Object injectionSource) {
        return injectionSource instanceof EnvEntryInjectionSource && equalTo((EnvEntryInjectionSource) injectionSource);
    }

    private boolean equalTo(final EnvEntryInjectionSource injectionSource) {
        return injectionSource != null && value.equals(injectionSource.value);
    }

    public int hashCode() {
        return value.hashCode();
    }
}
