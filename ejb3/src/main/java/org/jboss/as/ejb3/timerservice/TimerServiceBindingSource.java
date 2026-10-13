/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * An {@link InjectionSource} which returns a {@link ManagedReference reference} to a {@link jakarta.ejb.TimerService}.
 * <p>
 * At {@link org.jboss.as.version.Stability#COMMUNITY} or higher the factory creates an
 * {@link ExtendedTimerServiceImpl}, so the injected object is also castable to
 * {@link org.jboss.ejb3.timerservice.ExtendedTimerService}. At lower stability levels a plain
 * {@link TimerServiceImpl} is created, which does not implement that interface.
 * </p>
 *
 * @author Jaikiran Pai
 */
public class TimerServiceBindingSource extends InjectionSource {

    private static final TimerServiceManagedReferenceFactory INSTANCE = new TimerServiceManagedReferenceFactory();

    @Override
    public void getResourceValue(ResolutionContext resolutionContext, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        injector.inject(INSTANCE);
    }

    private static class TimerServiceManagedReferenceFactory implements ContextListManagedReferenceFactory {

        private static final TimerServiceManagedReference REFERENCE = new TimerServiceManagedReference();

        @Override
        public ManagedReference getReference() {
            return REFERENCE;
        }

        @Override
        public String getInstanceClassName() {
            return jakarta.ejb.TimerService.class.getName();
        }
    }

    private static class TimerServiceManagedReference implements ManagedReference {

        @Override
        public void release() {
        }

        @Override
        public Object getInstance() {
            final InterceptorContext currentInvocationContext = CurrentInvocationContext.get();
            final EJBComponent ejbComponent = (EJBComponent) currentInvocationContext.getPrivateData(Component.class);
            if (ejbComponent == null) {
                throw EjbLogger.EJB3_TIMER_LOGGER.failToGetEjbComponent(currentInvocationContext);
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
