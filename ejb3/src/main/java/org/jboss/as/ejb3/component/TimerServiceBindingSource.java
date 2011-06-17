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

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.spi.EJBComponent;
import org.jboss.ejb3.context.spi.InvocationContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * An {@link InjectionSource} which returns a {@link ManagedReference reference} to a {@link javax.ejb.TimerService}
 * <p/>
 * User: Jaikiran Pai
 */
public class TimerServiceBindingSource extends InjectionSource {

    private static final TimerServiceManagedReferenceFactory TIMER_SERVICE_MANAGED_REFERENCE_FACTORY_INSTANCE = new TimerServiceManagedReferenceFactory();

    @Override
    public void getResourceValue(ResolutionContext resolutionContext, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        injector.inject(TIMER_SERVICE_MANAGED_REFERENCE_FACTORY_INSTANCE);
    }

    /**
     * {@link ManagedReferenceFactory} for returning a {@link ManagedReference} to a {@link javax.ejb.TimerService}
     */
    private static class TimerServiceManagedReferenceFactory implements ManagedReferenceFactory {

        private final TimerServiceManagedReference timerServiceManagedReference = new TimerServiceManagedReference();

        @Override
        public ManagedReference getReference() {
            return timerServiceManagedReference;
        }
    }

    /**
     * A {@link ManagedReference} to a {@link javax.ejb.TimerService}
     */
    private static class TimerServiceManagedReference implements ManagedReference {

        @Override
        public void release() {

        }

        @Override
        public Object getInstance() {
            // get the current invocation context and the EJBComponent out of it
            final InvocationContext currentInvocationContext = CurrentInvocationContext.get();
            final EJBComponent ejbComponent = currentInvocationContext.getComponent();
            if (ejbComponent == null) {
                throw new IllegalStateException("EJBComponent has not been set in the current invocation context " + currentInvocationContext);
            }
            return ejbComponent.getTimerService();
        }
    }


    // All Timer bindings are equivalent since they just use a thread local context
    public boolean equals(Object o) {
        return o instanceof TimerServiceBindingSource;
    }

    public int hashCode() {
        return 1;
    }
}
