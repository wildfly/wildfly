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

import java.util.List;

import javax.management.MBeanServer;
import javax.management.j2ee.ManagementHome;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.DefaultEjbClientContextService;
import org.jboss.as.ejb3.remote.RemoteViewManagedReferenceFactory;
import org.jboss.as.ejb3.remote.TCCLBasedEJBClientContextSelector;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.Services;
import org.jboss.as.server.jmx.PluggableMBeanServer;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class JSR77ManagementSubsystemAdd extends AbstractAddStepHandler {

    static JSR77ManagementSubsystemAdd INSTANCE = new JSR77ManagementSubsystemAdd();


    private JSR77ManagementSubsystemAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                RegisterMBeanServerDelegateService mbeanServerService = new RegisterMBeanServerDelegateService();
                context.getServiceTarget().addService(RegisterMBeanServerDelegateService.SERVICE_NAME, mbeanServerService)
                    .addDependency(MBeanServerService.SERVICE_NAME, PluggableMBeanServer.class, mbeanServerService.injectedMbeanServer)
                    .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, mbeanServerService.injectedController)
                    .setInitialMode(Mode.ACTIVE)
                    .install();

                RegisterManagementEJBService managementEjbService = new RegisterManagementEJBService();
                context.getServiceTarget().addService(RegisterManagementEJBService.SERVICE_NAME, managementEjbService)
                    .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, managementEjbService.deploymentRepositoryValue)
                    .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, managementEjbService.mbeanServerValue)
                    //TODO I think these are needed here since we don't go through EjbClientContextSetupProcessor
                    .addDependency(DefaultEjbClientContextService.DEFAULT_SERVICE_NAME, EJBClientContext.class, managementEjbService.ejbClientContextValue)
                    .addDependency(TCCLBasedEJBClientContextSelector.TCCL_BASED_EJB_CLIENT_CONTEXT_SELECTOR_SERVICE_NAME, TCCLBasedEJBClientContextSelector.class, managementEjbService.ejbClientContextSelectorValue)
                    .setInitialMode(Mode.ACTIVE)
                    .install();

                //TODO null for source ok?
                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(JNDI_NAME);
                final BinderService binderService = new BinderService(bindInfo.getBindName(), null);
                context.getServiceTarget().addService(bindInfo.getBinderServiceName(), binderService)
                    .addInjection(binderService.getManagedObjectInjector(), new RemoteViewManagedReferenceFactory(APP_NAME, MODULE_NAME, DISTINCT_NAME, EJB_NAME, ManagementHome.class.getName(), false))
                    .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                    .setInitialMode(Mode.ACTIVE)
                    .install();

                context.completeStep();

            }
        }, Stage.RUNTIME);
    }


}
