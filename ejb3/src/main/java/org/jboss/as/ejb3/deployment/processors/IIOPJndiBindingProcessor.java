/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ejb3.iiop.handle.HandleDelegateImpl;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.omg.CORBA.ORB;
import org.wildfly.iiop.openjdk.deployment.IIOPDeploymentMarker;
import org.wildfly.iiop.openjdk.service.CorbaORBService;

/**
 * Processor responsible for binding IIOP related resources to JNDI.
 * </p>
 * Unlike other resource injections this binding happens for all eligible components,
 * regardless of the presence of the {@link javax.annotation.Resource} annotation.
 *
 * @author Stuart Douglas
 */
public class IIOPJndiBindingProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);

        if (moduleDescription == null) {
            return;
        }

        //do not bind if jacORB not present
        if (!IIOPDeploymentMarker.isIIOPDeployment(deploymentUnit)) {
            return;
        }

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        //if this is a war we need to bind to the modules comp namespace

        if (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit) || DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
            final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getApplicationName(), moduleDescription.getModuleName());
            bindService(serviceTarget, moduleContextServiceName, module);
        }

        for (ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            if (component.getNamingMode() == ComponentNamingMode.CREATE) {
                final ServiceName compContextServiceName = ContextNames.contextServiceNameOfComponent(moduleDescription.getApplicationName(), moduleDescription.getModuleName(), component.getComponentName());
                bindService(serviceTarget, compContextServiceName, module);
            }
        }

    }

    /**
     * Binds java:comp/ORB
     *
     * @param serviceTarget      The service target
     * @param contextServiceName The service name of the context to bind to
     */
    private void bindService(final ServiceTarget serviceTarget, final ServiceName contextServiceName, final Module module) {

        final ServiceName orbServiceName = contextServiceName.append("ORB");
        final BinderService orbService = new BinderService("ORB");
        serviceTarget.addService(orbServiceName, orbService)
                .addDependency(CorbaORBService.SERVICE_NAME, ORB.class,
                        new ManagedReferenceInjector<ORB>(orbService.getManagedObjectInjector()))
                .addDependency(contextServiceName, ServiceBasedNamingStore.class, orbService.getNamingStoreInjector())
                .install();


        final ServiceName handleDelegateServiceName = contextServiceName.append("HandleDelegate");
        final BinderService handleDelegateBindingService = new BinderService("HandleDelegate");
        handleDelegateBindingService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue(new HandleDelegateImpl(module.getClassLoader()))));
        serviceTarget.addService(handleDelegateServiceName, handleDelegateBindingService)
                .addDependency(contextServiceName, ServiceBasedNamingStore.class, handleDelegateBindingService.getNamingStoreInjector())
                .install();

    }


    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
