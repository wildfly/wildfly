/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ejb3.component.EJBUtilities;
import org.jboss.as.ejb3.deployment.processors.AccessTimeoutAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.ApplicationExceptionAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.AsynchronousAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.BusinessViewAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.ConcurrencyManagementAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.DeclareRolesProcessor;
import org.jboss.as.ejb3.deployment.processors.DenyAllProcessor;
import org.jboss.as.ejb3.deployment.processors.EJBComponentDescriptionFactory;
import org.jboss.as.ejb3.deployment.processors.EjbContextJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbDependencyDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbDependsOnAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJarConfigurationProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJarParsingDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJndiBindingsDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbRefProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbResourceInjectionAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.ImplicitLocalViewProcessor;
import org.jboss.as.ejb3.deployment.processors.LockAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.MethodPermissionDDProcessor;
import org.jboss.as.ejb3.deployment.processors.RemoveAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.ResourceAdapterAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.RolesAllowedProcessor;
import org.jboss.as.ejb3.deployment.processors.RunAsProcessor;
import org.jboss.as.ejb3.deployment.processors.SecurityDomainProcessor;
import org.jboss.as.ejb3.deployment.processors.SessionSynchronizationProcessor;
import org.jboss.as.ejb3.deployment.processors.StartupAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.StatefulTimeoutAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.TimerServiceJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.TransactionAttributeAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.TransactionManagementAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.AssemblyDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.DeploymentDescriptorInterceptorBindingsProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.DeploymentDescriptorMethodProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.EjbConcurrencyProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.ExcludeListDDProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.InterceptorClassDeploymentDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.MessageDrivenBeanXmlDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.RemoveMethodDeploymentDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SecurityIdentityDDProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SecurityRoleRefDDProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SessionBeanXmlDescriptorProcessor;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.txn.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Emanuel Muckenhuber
 */
class EJB3SubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final EJB3SubsystemAdd INSTANCE = new EJB3SubsystemAdd();

    private EJB3SubsystemAdd() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));

        if (context instanceof BootOperationContext) {
            final BootOperationContext updateContext = (BootOperationContext) context;

            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();
                    final EJBUtilities utilities = new EJBUtilities();
                    serviceTarget.addService(EJBUtilities.SERVICE_NAME, utilities)
                            .addDependency(SimpleSecurityManagerService.SERVICE_NAME, SimpleSecurityManager.class, utilities.getSecurityManagerInjector())
                            .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, utilities.getTransactionManagerInjector())
                            .addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, utilities.getTransactionSynchronizationRegistryInjector())
                            .addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION, UserTransaction.class, utilities.getUserTransactionInjector())
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .install();
                    resultHandler.handleResultComplete(); // TODO: Listener
                }
            });

            // add the metadata parser deployment processor
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DEPLOYMENT, new EjbJarParsingDeploymentUnitProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CREATE_COMPONENT_DESCRIPTIONS, new EJBComponentDescriptionFactory());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SESSION_BEAN_DD, new SessionBeanXmlDescriptorProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_MDB_DD, new MessageDrivenBeanXmlDescriptorProcessor());
            //updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ANNOTATION, new EjbAnnotationProcessor());
            //updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MESSAGE_DRIVEN_ANNOTATION, new MessageDrivenAnnotationProcessor());
            // Process @DependsOn after the @Singletons have been registered.
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CONTEXT_BINDING, new EjbContextJndiBindingProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_TIMERSERVICE_BINDING, new TimerServiceJndiBindingProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_TRANSACTION_MANAGEMENT, new TransactionManagementAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_BUSINESS_VIEW_ANNOTATION, new BusinessViewAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_INJECTION_ANNOTATION, new EjbResourceInjectionAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_STARTUP_ANNOTATION, new StartupAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CONCURRENCY_MANAGEMENT_ANNOTATION, new ConcurrencyManagementAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_LOCK_ANNOTATION, new LockAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DECLARE_ROLES_ANNOTATION, new DeclareRolesProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_RUN_AS_ANNOTATION, new RunAsProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DENY_ALL_ANNOTATION, new DenyAllProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ROLES_ALLOWED_ANNOTATION, new RolesAllowedProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_STATEFUL_TIMEOUT_ANNOTATION, new StatefulTimeoutAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ACCESS_TIMEOUT_ANNOTATION, new AccessTimeoutAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_TRANSACTION_ATTR_ANNOTATION, new TransactionAttributeAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SESSION_SYNCHRONIZATION, new SessionSynchronizationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_RESOURCE_ADAPTER_ANNOTATION, new ResourceAdapterAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ASYNCHRONOUS_ANNOTATION, new AsynchronousAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_APPLICATION_EXCEPTION_ANNOTATION, new ApplicationExceptionAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_REMOVE_METHOD_ANNOTAION, new RemoveAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DD_INTERCEPTORS, new InterceptorClassDeploymentDescriptorProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ASSEMBLY_DESC_DD, new AssemblyDescriptorProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SECURITY_ROLE_REF_DD, new SecurityRoleRefDDProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SECURITY_IDENTITY_DD, new SecurityIdentityDDProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SECURITY_DOMAIN_ANNOTATION, new SecurityDomainProcessor());


            updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_EJB, new EjbDependencyDeploymentUnitProcessor());

            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_IMPLICIT_NO_INTERFACE_VIEW, new ImplicitLocalViewProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_JNDI_BINDINGS, new EjbJndiBindingsDeploymentUnitProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_MODULE_CONFIGURATION, new EjbJarConfigurationProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_INTERCEPTORS, new DeploymentDescriptorInterceptorBindingsProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_CONCURRENCY, new EjbConcurrencyProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_METHOD_RESOLUTION, new DeploymentDescriptorMethodProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_REMOVE_METHOD, new RemoveMethodDeploymentDescriptorProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_EXCLUDE_LIST_DD, new ExcludeListDDProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_METHOD_PERMISSION_DD, new MethodPermissionDDProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_REF, new EjbRefProcessor());

            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_DEPENDS_ON_ANNOTATION, new EjbDependsOnAnnotationProcessor());


            // add the real deployment processor
            // TODO: add the proper deployment processors
            // updateContext.addDeploymentProcessor(processor, priority);
        }

        context.getSubModel().setEmptyObject();
        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOperation);
    }

}
