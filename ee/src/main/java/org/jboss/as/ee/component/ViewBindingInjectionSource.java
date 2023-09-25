/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ViewBindingInjectionSource extends InjectionSource {

    private final ServiceName serviceName;

    public ViewBindingInjectionSource(final ServiceName serviceName) {
        this.serviceName = serviceName;
    }

    /** {@inheritDoc} */
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        serviceBuilder.addDependency(serviceName, ComponentView.class, new ViewManagedReferenceFactory.Injector(injector));
    }

    public boolean equals(final Object injectionSource) {
         return injectionSource instanceof ViewBindingInjectionSource && equalTo((ViewBindingInjectionSource) injectionSource);
    }

    private boolean equalTo(final ViewBindingInjectionSource configuration) {
        return configuration != null && serviceName.equals(configuration.serviceName);
    }

    public int hashCode() {
        return serviceName.hashCode();
    }

}
