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
import org.jboss.as.ejb3.iiop.EjbIIOPService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ear.spec.Ear6xMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
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
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (eeModuleDescription == null) {
            return;
        }
        // Note, we do not use the EEModuleDescription.getApplicationName() because that API returns the
        // module name if the top level unit isn't a .ear, which is not what we want. We really want a
        // .ear name as application name (that's the semantic in EJB spec).
        String applicationName = this.getApplicationName(deploymentUnit);
        // if it's not a .ear deployment then set app name to empty string
        applicationName = applicationName == null ? "" : applicationName;
        final DeploymentModuleIdentifier identifier = new DeploymentModuleIdentifier(applicationName, eeModuleDescription.getModuleName(), eeModuleDescription.getDistinctName());

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
                final InjectedValue<EjbIIOPService> iorFactory = new InjectedValue<EjbIIOPService>();
                if(ejbComponentDescription.isExposedViaIiop()) {
                    injectedValues.put(ejbComponentDescription.getServiceName().append(EjbIIOPService.SERVICE_NAME), iorFactory);
                }

                EjbDeploymentInformation info = new EjbDeploymentInformation(ejbComponentDescription.getEJBName(), componentInjectedValue, views, module.getClassLoader(), iorFactory);
                deploymentInformationMap.put(ejbComponentDescription.getEJBName(), info);
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

    /**
     * Returns the application name for the passed deployment. If the passed deployment isn't an .ear or doesn't belong
     * to a .ear, then this method returns null. Else it returns the application-name set in the application.xml of the .ear
     * or if that's not set, will return the .ear deployment unit name (stripped off the .ear suffix).
     *
     * @param deploymentUnit The deployment unit
     */
    private String getApplicationName(DeploymentUnit deploymentUnit) {
        final DeploymentUnit parentDU = deploymentUnit.getParent();
        if (parentDU == null) {
            final EarMetaData earMetaData = deploymentUnit.getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
            if (earMetaData != null && earMetaData instanceof Ear6xMetaData) {
                final String overriddenAppName = ((Ear6xMetaData) earMetaData).getApplicationName();
                if (overriddenAppName == null) {
                    return this.getEarName(deploymentUnit);
                }
                return overriddenAppName;
            } else {
                return this.getEarName(deploymentUnit);
            }
        }
        // traverse to top level DU
        return this.getApplicationName(parentDU);
    }

    /**
     * Returns the name (stripped off the .ear suffix) of the passed <code>deploymentUnit</code>.
     * Returns null if the passed <code>deploymentUnit</code>'s name doesn't end with .ear suffix.
     *
     * @param deploymentUnit Deployment unit
     * @return
     */
    private String getEarName(final DeploymentUnit deploymentUnit) {
        final String duName = deploymentUnit.getName();
        if (duName.endsWith(".ear")) {
            return duName.substring(0, duName.length() - ".ear".length());
        }
        return null;
    }
}
