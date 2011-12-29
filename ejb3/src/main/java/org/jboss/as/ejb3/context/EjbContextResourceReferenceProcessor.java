package org.jboss.as.ejb3.context;

import javax.ejb.EJBContext;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessor;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * Handler for EjbContext JNDI bindings
 *
 * @author Stuart Douglas
 */
public class EjbContextResourceReferenceProcessor implements EEResourceReferenceProcessor {

    private final Class<? extends EJBContext> type;

    public EjbContextResourceReferenceProcessor(final Class<? extends EJBContext> type) {
        this.type = type;
    }

    @Override
    public String getResourceReferenceType() {
        return type.getName();
    }

    @Override
    public InjectionSource getResourceReferenceBindingSource() throws DeploymentUnitProcessingException {
        return new EjbContextInjectionSource();
    }

    private static final ManagedReference ejbContextManagedReference = new ManagedReference() {
        public void release() {
        }

        public Object getInstance() {
            return CurrentInvocationContext.getEjbContext();
        }
    };

    private static final ManagedReferenceFactory ejbContextManagedReferenceFactory = new ManagedReferenceFactory() {
        public ManagedReference getReference() {
            return ejbContextManagedReference;
        }
    };

    private static class EjbContextInjectionSource extends InjectionSource {

        @Override
        public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
            injector.inject(ejbContextManagedReferenceFactory);
        }

        public boolean equals(Object other) {
            return other instanceof EjbContextInjectionSource;
        }

        public int hashCode() {
            return 45647;
        }
    }
}
