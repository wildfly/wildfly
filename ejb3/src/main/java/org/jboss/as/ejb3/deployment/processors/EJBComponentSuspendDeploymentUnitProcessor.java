package org.jboss.as.ejb3.deployment.processors;

import java.util.EnumSet;
import java.util.Set;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewConfiguration;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.ControlPointService;
import org.wildfly.extension.requestcontroller.RequestControllerActivationMarker;


/**
 * @author Stuart Douglas
 */
public class EJBComponentSuspendDeploymentUnitProcessor implements DeploymentUnitProcessor {

    public static final String ENTRY_POINT_NAME = "ejb.";
    static final Set<MethodIntf> INTERFACES = EnumSet.of(MethodIntf.REMOTE, MethodIntf.HOME, MethodIntf.MESSAGE_ENDPOINT);

    @Override
    public void deploy(DeploymentPhaseContext context) {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final String topLevelName;
        //check if the controller is installed
        if (!RequestControllerActivationMarker.isRequestControllerEnabled(deploymentUnit)) {
            return;
        }
        if (deploymentUnit.getParent() == null) {
            topLevelName = deploymentUnit.getName();
        } else {
            topLevelName = deploymentUnit.getParent().getName();
        }
        for (ComponentDescription component : deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION).getComponentDescriptions()) {
            if (component instanceof EJBComponentDescription) {
                final String entryPoint = ENTRY_POINT_NAME + deploymentUnit.getName() + "." + component.getComponentName();
                ControlPointService.install(context.getServiceTarget(), topLevelName, entryPoint);
                component.getConfigurators().add(new ComponentConfigurator() {
                    @Override
                    public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) {
                        EjbSuspendInterceptor interceptor = null;
                        ImmediateInterceptorFactory factory = null;
                        for (ViewConfiguration view: configuration.getViews()) {
                            EJBViewConfiguration ejbView = (EJBViewConfiguration) view;
                            if (INTERFACES.contains(ejbView.getMethodIntf())) {
                                if (factory == null) {
                                    interceptor = new EjbSuspendInterceptor();
                                    factory = new ImmediateInterceptorFactory(interceptor);
                                }
                                view.addViewInterceptor(factory, InterceptorOrder.View.GRACEFUL_SHUTDOWN);
                            }
                        }

                        configuration.getCreateDependencies().add(new DependencyConfigurator<EJBComponentCreateService>() {
                            @Override
                            public void configureDependency(ServiceBuilder<?> serviceBuilder, EJBComponentCreateService service) {
                                serviceBuilder.addDependency(ControlPointService.serviceName(topLevelName, entryPoint), ControlPoint.class, service.getControlPointInjector());
                            }
                        });

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
