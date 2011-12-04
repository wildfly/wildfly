package org.jboss.as.ejb3.deployment.processors.merging;

import javax.ejb.SessionBean;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.session.SessionBeanSetSessionContextMethodInvocationInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;

/**
 * Processor that handles the {@link javax.ejb.SessionBean} interface
 *
 * @author Stuart Douglas
 */
public class SessionBeanMergingProcessor extends AbstractMergingProcessor<SessionBeanComponentDescription> {

    public SessionBeanMergingProcessor() {
        super(SessionBeanComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription description) throws DeploymentUnitProcessingException {

    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription description) throws DeploymentUnitProcessingException {
        if (SessionBean.class.isAssignableFrom(componentClass)) {
            // add the setSessionContext(SessionContext) method invocation interceptor for session bean implementing the javax.ejb.SessionContext
            // interface
            description.getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    if (SessionBean.class.isAssignableFrom(configuration.getComponentClass())) {
                        configuration.addPostConstructInterceptor(SessionBeanSetSessionContextMethodInvocationInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SET_CONTEXT_METHOD_INVOCATION_INTERCEPTOR);
                    }
                }
            });

            //now EJB remove
            final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
            builder.setPreDestroy(MethodIdentifier.getIdentifier(void.class, "ejbRemove"));
            description.addInterceptorMethodOverride(componentClass.getName(), builder.build());

        }
    }
}
