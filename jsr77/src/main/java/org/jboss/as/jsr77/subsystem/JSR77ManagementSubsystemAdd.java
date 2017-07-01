/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
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
package org.jboss.as.jsr77.subsystem;

import static org.jboss.as.jsr77.subsystem.Constants.APP_NAME;
import static org.jboss.as.jsr77.subsystem.Constants.DISTINCT_NAME;
import static org.jboss.as.jsr77.subsystem.Constants.EJB_NAME;
import static org.jboss.as.jsr77.subsystem.Constants.JNDI_NAME;
import static org.jboss.as.jsr77.subsystem.Constants.MODULE_NAME;

import javax.management.MBeanServer;
import javax.management.j2ee.ManagementHome;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.EJBClientContextService;
import org.jboss.as.ejb3.remote.RemoteViewManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.jmx.PluggableMBeanServer;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Values;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class JSR77ManagementSubsystemAdd extends AbstractBoottimeAddStepHandler {

    JSR77ManagementSubsystemAdd(boolean appclient) {
        super(appclient ? JSR77ManagementRootResource.JSR77_APPCLIENT_CAPABILITY : JSR77ManagementRootResource.JSR77_CAPABILITY);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(JSR77ManagementExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JSR77, new Jsr77DependenciesProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;

        if(!appclient) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    ServiceTarget target = context.getServiceTarget();

                    ServiceName mbeanServerServiceName = context.getCapabilityServiceName(JSR77ManagementRootResource.JMX_CAPABILITY, MBeanServer.class);

                    RegisterMBeanServerDelegateService mbeanServerService = new RegisterMBeanServerDelegateService();
                    target.addService(RegisterMBeanServerDelegateService.SERVICE_NAME, mbeanServerService)
                            .addDependency(mbeanServerServiceName, PluggableMBeanServer.class, mbeanServerService.injectedMbeanServer)
                            .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, mbeanServerService.injectedController)
                            .setInitialMode(Mode.ACTIVE)
                            .install();


                    RegisterManagementEJBService managementEjbService = new RegisterManagementEJBService();
                    target.addService(RegisterManagementEJBService.SERVICE_NAME, managementEjbService)
                            .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, managementEjbService.deploymentRepositoryValue)
                            .addDependency(mbeanServerServiceName, MBeanServer.class, managementEjbService.mbeanServerValue)
                                    //TODO I think this is needed here since we don't go through EjbClientContextSetupProcessor
                            .addDependency(EJBClientContextService.DEFAULT_SERVICE_NAME, EJBClientContextService.class, managementEjbService.ejbClientContextValue)
                            .addDependency(AssociationService.SERVICE_NAME, AssociationService.class, managementEjbService.associationServiceInjector)
                            .setInitialMode(Mode.PASSIVE)
                            .install();

                    //TODO null for source ok?
                    final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(JNDI_NAME);
                    final BinderService binderService = new BinderService(bindInfo.getBindName(), null);
                    final InjectedValue<ClassLoader> viewClassLoader = new InjectedValue<ClassLoader>();
                    viewClassLoader.setValue(Values.immediateValue(ManagementHome.class.getClassLoader()));
                    target.addService(bindInfo.getBinderServiceName(), binderService)
                            .addInjection(binderService.getManagedObjectInjector(), new RemoteViewManagedReferenceFactory(APP_NAME, MODULE_NAME, DISTINCT_NAME, EJB_NAME, ManagementHome.class.getName(), false, viewClassLoader, appclient))
                            .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                            .setInitialMode(Mode.ACTIVE)
                            .install();

                    // Rollback is handled by the parent step
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);

                }
            }, Stage.RUNTIME);
        }
    }
}
