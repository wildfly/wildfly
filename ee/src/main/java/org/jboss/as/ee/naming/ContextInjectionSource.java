package org.jboss.as.ee.naming;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ContextManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingMessages;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 *
 * Injection source that can be used to bind a potentially empty context
 *
* @author Stuart Douglas
*/
public class ContextInjectionSource extends InjectionSource {

    private final String name;
    private final String fullName;

    public ContextInjectionSource(final String name, String fullName) {
        this.name = name;
        this.fullName = fullName;
    }

    @Override
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final ContextManagedReferenceFactory managedReferenceFactory = new ContextManagedReferenceFactory(name);
        final ServiceName contextServiceName;
        if(fullName.startsWith("java:app")) {
            contextServiceName = ContextNames.contextServiceNameOfApplication(resolutionContext.getApplicationName());
        } else if (fullName.startsWith("java:module") || (fullName.startsWith("java:comp") && resolutionContext.isCompUsesModule())) {
            contextServiceName = ContextNames.contextServiceNameOfModule(resolutionContext.getApplicationName(), resolutionContext.getModuleName());
        } else if(fullName.startsWith("java:comp")) {
            contextServiceName = ContextNames.contextServiceNameOfComponent(resolutionContext.getApplicationName(), resolutionContext.getModuleName(), resolutionContext.getComponentName());
        } else {
            throw NamingMessages.MESSAGES.invalidNameForContextBinding(fullName);
        }
        serviceBuilder.addDependency(contextServiceName, NamingStore.class, managedReferenceFactory.getNamingStoreInjectedValue());
        injector.inject(managedReferenceFactory);
    }
}
