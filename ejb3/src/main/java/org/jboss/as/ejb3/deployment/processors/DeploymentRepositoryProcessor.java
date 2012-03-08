package org.jboss.as.ejb3.deployment.processors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.iiop.EjbIIOPService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class DeploymentRepositoryProcessor implements DeploymentUnitProcessor {


    public DeploymentRepositoryProcessor() {
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (eeModuleDescription == null) {
            return;
        }
        // Note, we do not use the EEModuleDescription.getApplicationName() because that API returns the
        // module name if the top level unit isn't a .ear, which is not what we want. We really want a
        // .ear name as application name (that's the semantic in EJB spec). So use EEModuleDescription.getEarApplicationName
        String applicationName = eeModuleDescription.getEarApplicationName();
        // if it's not a .ear deployment then set app name to empty string
        applicationName = applicationName == null ? "" : applicationName;
        final DeploymentModuleIdentifier identifier = new DeploymentModuleIdentifier(applicationName, eeModuleDescription.getModuleName(), eeModuleDescription.getDistinctName());

        final Collection<ComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        final Map<String, EjbDeploymentInformation> deploymentInformationMap = new HashMap<String, EjbDeploymentInformation>();

        final Map<ServiceName, InjectedValue<?>> injectedValues = new HashMap<ServiceName, InjectedValue<?>>();

        for (final ComponentDescription component : componentDescriptions) {
            if (component instanceof EJBComponentDescription) {
                final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) component;

                final InjectedValue<EJBComponent> componentInjectedValue = new InjectedValue<EJBComponent>();
                injectedValues.put(component.getCreateServiceName(), componentInjectedValue);
                final Map<String, InjectedValue<ComponentView>> remoteViews = new HashMap<String, InjectedValue<ComponentView>>();
                final Map<String, InjectedValue<ComponentView>> localViews = new HashMap<String, InjectedValue<ComponentView>>();
                for (final ViewDescription view : ejbComponentDescription.getViews()) {
                    boolean remoteView = false;
                    if (view instanceof EJBViewDescription) {
                        final MethodIntf viewType = ((EJBViewDescription) view).getMethodIntf();
                        if (viewType == MethodIntf.HOME || viewType == MethodIntf.REMOTE) {
                            remoteView = true;
                        }
                    }
                    final InjectedValue<ComponentView> componentViewInjectedValue = new InjectedValue<ComponentView>();
                    if (remoteView) {
                        remoteViews.put(view.getViewClassName(), componentViewInjectedValue);
                    } else {
                        localViews.put(view.getViewClassName(), componentViewInjectedValue);
                    }
                    injectedValues.put(view.getServiceName(), componentViewInjectedValue);
                }
                final InjectedValue<EjbIIOPService> iorFactory = new InjectedValue<EjbIIOPService>();
                if (ejbComponentDescription.isExposedViaIiop()) {
                    injectedValues.put(ejbComponentDescription.getServiceName().append(EjbIIOPService.SERVICE_NAME), iorFactory);
                }

                final EjbDeploymentInformation info = new EjbDeploymentInformation(ejbComponentDescription.getEJBName(), componentInjectedValue, remoteViews, localViews, module.getClassLoader(), iorFactory);
                deploymentInformationMap.put(ejbComponentDescription.getEJBName(), info);
            }
        }

        final ModuleDeployment deployment = new ModuleDeployment(identifier, deploymentInformationMap);
        final ServiceBuilder<ModuleDeployment> builder = phaseContext.getServiceTarget().addService(deploymentUnit.getServiceName().append(ModuleDeployment.SERVICE_NAME), deployment);
        for (Map.Entry<ServiceName, InjectedValue<?>> entry : injectedValues.entrySet()) {
            builder.addDependency(entry.getKey(), (InjectedValue<Object>) entry.getValue());
        }
        builder.addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, deployment.getDeploymentRepository());
        builder.install();


    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }

}
