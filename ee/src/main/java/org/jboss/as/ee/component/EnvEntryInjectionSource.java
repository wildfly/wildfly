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
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.Values;

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
        injector.inject(new ValueManagedReferenceFactory(Values.immediateValue(value)));
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
