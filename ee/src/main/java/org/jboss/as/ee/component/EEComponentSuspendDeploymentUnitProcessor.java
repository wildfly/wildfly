package org.jboss.as.ee.component;

import org.jboss.as.ee.component.interceptors.ComponentSuspenendInterceptor;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.requestcontroller.ControlPoint;
import org.jboss.as.server.requestcontroller.EntryPointService;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;


/**
 * @author Stuart Douglas
 */
public class EEComponentSuspendDeploymentUnitProcessor implements DeploymentUnitProcessor {

    public static final String ENTRY_POINT_NAME = "ee-component";

    @Override
    public void deploy(DeploymentPhaseContext context) {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final String topLevelName;
        if(deploymentUnit.getParent() == null) {
        EntryPointService.install(context.getServiceTarget(), deploymentUnit.getName(), ENTRY_POINT_NAME);
            topLevelName = deploymentUnit.getName();
        } else {
            topLevelName = deploymentUnit.getParent().getName();
        }
        for(ComponentDescription component : deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION).getComponentDescriptions()) {
            if(component.isIntercepted()) {
                component.getConfigurators().add(new ComponentConfigurator() {
                    @Override
                    public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

                        final ComponentSuspenendInterceptor interceptor = new ComponentSuspenendInterceptor();
                        configuration.getCreateDependencies().add(new DependencyConfigurator<Service<Component>>() {
                            @Override
                            public void configureDependency(ServiceBuilder<?> serviceBuilder, Service<Component> service) throws DeploymentUnitProcessingException {
                                serviceBuilder.addDependency(EntryPointService.serviceName(topLevelName, ENTRY_POINT_NAME), ControlPoint.class, interceptor.getEntryPointInjectedValue());
                            }
                        });

                        ImmediateInterceptorFactory factory = new ImmediateInterceptorFactory(interceptor);
                        if(description.isTimerServiceApplicable()) {
                            configuration.addTimeoutViewInterceptor(factory, InterceptorOrder.View.SHUTDOWN_INTERCEPTOR);
                        }
                        for(ViewConfiguration view: configuration.getViews()) {
                            view.addViewInterceptor(factory, InterceptorOrder.View.SHUTDOWN_INTERCEPTOR);
                        }

                    }
                });
            }
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Do nothing
    }
}
