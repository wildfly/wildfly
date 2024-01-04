/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.injection;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.ws.common.injection.ThreadLocalAwareWebServiceContext;

/**
 * {@link InjectionSource} for {@link jakarta.xml.ws.WebServiceContext} resource.
 *
 * User: Jaikiran Pai
 */
public class WebServiceContextInjectionSource extends InjectionSource {
    @Override
    public void getResourceValue(ResolutionContext resolutionContext, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        injector.inject(new WebServiceContextManagedReferenceFactory());
    }

    private class WebServiceContextManagedReferenceFactory implements ManagedReferenceFactory {

        @Override
        public ManagedReference getReference() {
            return new WebServiceContextManagedReference();
        }
    }

    private class WebServiceContextManagedReference implements ManagedReference {

        @Override
        public void release() {
        }

        @Override
        public Object getInstance() {
            // return the WebServiceContext
            return ThreadLocalAwareWebServiceContext.getInstance();
        }
    }

    // all context injection sources are equal since they are thread locals
    public boolean equals(Object o) {
        return o instanceof WebServiceContextInjectionSource;
    }

    public int hashCode() {
        return 1;
    }
}
