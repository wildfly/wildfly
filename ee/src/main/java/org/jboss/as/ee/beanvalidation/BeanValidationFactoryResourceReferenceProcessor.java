package org.jboss.as.ee.beanvalidation;

import javax.validation.ValidatorFactory;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessor;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.ImmediateValue;

/**
 * Handled resource injections for the Validator Factory
 *
 * @author Stuart Douglas
 */
public class BeanValidationFactoryResourceReferenceProcessor implements EEResourceReferenceProcessor {

    public static final BeanValidationFactoryResourceReferenceProcessor INSTANCE = new BeanValidationFactoryResourceReferenceProcessor();

    @Override
    public String getResourceReferenceType() {
        return ValidatorFactory.class.getName();
    }

    @Override
    public InjectionSource getResourceReferenceBindingSource() throws DeploymentUnitProcessingException {
        return ValidatorFactoryInjectionSource.INSTANCE;
    }

    private static final class ValidatorFactoryInjectionSource extends InjectionSource {

        public static final ValidatorFactoryInjectionSource INSTANCE = new ValidatorFactoryInjectionSource();

        @Override
        public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
            final ClassLoader classLoader = phaseContext.getDeploymentUnit().getAttachment(Attachments.MODULE).getClassLoader();
            injector.inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(new LazyValidatorFactory(classLoader))));
        }
    }
}
