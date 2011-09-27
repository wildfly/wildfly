package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
public class DeploymentRepositoryProcessor implements DeploymentUnitProcessor {


    public DeploymentRepositoryProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (eeModuleDescription == null) {
            return;
        }
        final DeploymentModuleIdentifier identifier = new DeploymentModuleIdentifier(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getDistinctName());

        final Collection<ComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        final Map<String, EjbDeploymentInformation> deploymentInformationMap = new HashMap<String, EjbDeploymentInformation>();

        final Map<ServiceName, InjectedValue<?>> injectedValues = new HashMap<ServiceName, InjectedValue<?>>();

        for(final ComponentDescription component : componentDescriptions) {
            if(component instanceof EJBComponentDescription) {
                final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) component;

                final InjectedValue<EJBComponent> componentInjectedValue = new InjectedValue<EJBComponent>();
                injectedValues.put(component.getCreateServiceName(), componentInjectedValue);
                final Map<String, InjectedValue<ComponentView>> views = new HashMap<String, InjectedValue<ComponentView>>();
                for(ViewDescription view : ejbComponentDescription.getViews()) {
                    final InjectedValue<ComponentView> componentViewInjectedValue = new InjectedValue<ComponentView>();
                    views.put(view.getViewClassName(), componentViewInjectedValue);
                    injectedValues.put(view.getServiceName(), componentViewInjectedValue);
                }
                EjbDeploymentInformation info = new EjbDeploymentInformation(ejbComponentDescription.getEJBName(), componentInjectedValue, views, module.getClassLoader());
                deploymentInformationMap.put(ejbComponentDescription.getEJBClassName(), info);
            }
        }

        final ModuleDeployment deployment = new ModuleDeployment(identifier, deploymentInformationMap);
        final ServiceBuilder<ModuleDeployment> builder = phaseContext.getServiceTarget().addService(deploymentUnit.getServiceName().append(ModuleDeployment.SERVICE_NAME), deployment);
        for(Map.Entry<ServiceName, InjectedValue<?>> entry : injectedValues.entrySet()) {
            builder.addDependency(entry.getKey(), (InjectedValue<Object>)entry.getValue());
        }
        builder.addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, deployment.getDeploymentRepository());
        builder.install();


    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }
}
