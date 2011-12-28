package org.jboss.as.ee.beanvalidation;

import javax.validation.Validator;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessor;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * Handled resource injections for the Validator
 *
 * @author Stuart Douglas
 */
public class BeanValidationResourceReferenceProcessor implements EEResourceReferenceProcessor {

    public static final BeanValidationResourceReferenceProcessor INSTANCE = new BeanValidationResourceReferenceProcessor();

    @Override
    public String getResourceReferenceType() {
        return Validator.class.getName();
    }

    @Override
    public InjectionSource getResourceReferenceBindingSource() throws DeploymentUnitProcessingException {
        return ValidatorInjectionSource.INSTANCE;
    }

    private static final class ValidatorInjectionSource extends InjectionSource {

        public static final ValidatorInjectionSource INSTANCE = new ValidatorInjectionSource();

        @Override
        public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
            final ClassLoader classLoader = phaseContext.getDeploymentUnit().getAttachment(Attachments.MODULE).getClassLoader();
            injector.inject(new ValidatorJndiInjectable(new LazyValidatorFactory(classLoader)));
        }
    }
}
