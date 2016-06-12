package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewConfiguration;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.invocation.ImmediateInterceptorFactory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Adds  StartupAwaitInterceptor to exposed methods of EJB, forcing users to wait until all startup beans in the deployment are done with post-construct methods.
 * @author Fedor Gavrilov
 */
// adding an abstraction for the whole deployment unit to depend on while blocking external client calls is a better solution probably
// it requires a lot of rewriting in EJB code right now, hence this class to satisfy EJB 3.1 spec, section 4.8.1
// feel free to remove this class as well as StartupAwaitInterceptor and StartupCountDownInterceptor when if easier way to satisfy spec will appear
public class StartupAwaitDeploymentUnitProcessor implements DeploymentUnitProcessor {
  private static final Set<MethodIntf> INTFS = EnumSet.of(MethodIntf.MESSAGE_ENDPOINT, MethodIntf.REMOTE, MethodIntf.SERVICE_ENDPOINT, MethodIntf.LOCAL);

  @Override
  public void deploy(final DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
    final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
    final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
    for (ComponentDescription component : moduleDescription.getComponentDescriptions()) {
      if (component instanceof EJBComponentDescription) {
        component.getConfigurators().add(new ComponentConfigurator() {
          @Override
          public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) {
            StartupCountdown countdown = context.getDeploymentUnit().getAttachment(Attachments.STARTUP_COUNTDOWN);
            for (ViewConfiguration view : configuration.getViews()) {
              EJBViewConfiguration ejbView = (EJBViewConfiguration) view;
              if (INTFS.contains(ejbView.getMethodIntf())) {
                ejbView.addViewInterceptor(new ImmediateInterceptorFactory(new StartupAwaitInterceptor(countdown)), InterceptorOrder.View.STARTUP_AWAIT_INTERCEPTOR);
              }
            }
          }
        });
      }
    }
  }

  @Override
  public void undeploy(DeploymentUnit context) {
  }
}