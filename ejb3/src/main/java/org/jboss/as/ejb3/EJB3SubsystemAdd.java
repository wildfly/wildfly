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

import java.util.List;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
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
import org.jboss.as.ejb3.deployment.processors.PermitAllProcessor;
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
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.txn.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
class EJB3SubsystemAdd extends AbstractAddStepHandler {

    static final EJB3SubsystemAdd INSTANCE = new EJB3SubsystemAdd();

    private EJB3SubsystemAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performRuntime(NewOperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        if (context.isBooting()) {
            context.addStep(new AbstractDeploymentChainStep() {
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    // add the metadata parser deployment processor
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DEPLOYMENT, new EjbJarParsingDeploymentUnitProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CREATE_COMPONENT_DESCRIPTIONS, new EJBComponentDescriptionFactory());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SESSION_BEAN_DD, new SessionBeanXmlDescriptorProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_MDB_DD, new MessageDrivenBeanXmlDescriptorProcessor());
                    //processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ANNOTATION, new EjbAnnotationProcessor());
                    //processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MESSAGE_DRIVEN_ANNOTATION, new MessageDrivenAnnotationProcessor());
                    // Process @DependsOn after the @Singletons have been registered.
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CONTEXT_BINDING, new EjbContextJndiBindingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_TIMERSERVICE_BINDING, new TimerServiceJndiBindingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_TRANSACTION_MANAGEMENT, new TransactionManagementAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_BUSINESS_VIEW_ANNOTATION, new BusinessViewAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_INJECTION_ANNOTATION, new EjbResourceInjectionAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_STARTUP_ANNOTATION, new StartupAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CONCURRENCY_MANAGEMENT_ANNOTATION, new ConcurrencyManagementAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_LOCK_ANNOTATION, new LockAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DECLARE_ROLES_ANNOTATION, new DeclareRolesProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_RUN_AS_ANNOTATION, new RunAsProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DENY_ALL_ANNOTATION, new DenyAllProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ROLES_ALLOWED_ANNOTATION, new RolesAllowedProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_STATEFUL_TIMEOUT_ANNOTATION, new StatefulTimeoutAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ACCESS_TIMEOUT_ANNOTATION, new AccessTimeoutAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_TRANSACTION_ATTR_ANNOTATION, new TransactionAttributeAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SESSION_SYNCHRONIZATION, new SessionSynchronizationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_RESOURCE_ADAPTER_ANNOTATION, new ResourceAdapterAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ASYNCHRONOUS_ANNOTATION, new AsynchronousAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_APPLICATION_EXCEPTION_ANNOTATION, new ApplicationExceptionAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_REMOVE_METHOD_ANNOTAION, new RemoveAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DD_INTERCEPTORS, new InterceptorClassDeploymentDescriptorProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ASSEMBLY_DESC_DD, new AssemblyDescriptorProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SECURITY_ROLE_REF_DD, new SecurityRoleRefDDProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SECURITY_IDENTITY_DD, new SecurityIdentityDDProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SECURITY_DOMAIN_ANNOTATION, new SecurityDomainProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_PERMIT_ALL_ANNOTATION, new PermitAllProcessor());


                    processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_EJB, new EjbDependencyDeploymentUnitProcessor());

                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_IMPLICIT_NO_INTERFACE_VIEW, new ImplicitLocalViewProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_JNDI_BINDINGS, new EjbJndiBindingsDeploymentUnitProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_MODULE_CONFIGURATION, new EjbJarConfigurationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_INTERCEPTORS, new DeploymentDescriptorInterceptorBindingsProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_CONCURRENCY, new EjbConcurrencyProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_METHOD_RESOLUTION, new DeploymentDescriptorMethodProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_REMOVE_METHOD, new RemoveMethodDeploymentDescriptorProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_EXCLUDE_LIST_DD, new ExcludeListDDProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_METHOD_PERMISSION_DD, new MethodPermissionDDProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_REF, new EjbRefProcessor());

                    processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_DEPENDS_ON_ANNOTATION, new EjbDependsOnAnnotationProcessor());


                    // add the real deployment processor
                    // TODO: add the proper deployment processors
                    // processorTarget.addDeploymentProcessor(processor, priority);
                }
            }, NewOperationContext.Stage.RUNTIME);
        }

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final EJBUtilities utilities = new EJBUtilities();
        newControllers.add(serviceTarget.addService(EJBUtilities.SERVICE_NAME, utilities)
                .addDependency(SimpleSecurityManagerService.SERVICE_NAME, SimpleSecurityManager.class, utilities.getSecurityManagerInjector())
                .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, utilities.getTransactionManagerInjector())
                .addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, utilities.getTransactionSynchronizationRegistryInjector())
                .addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION, UserTransaction.class, utilities.getUserTransactionInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());


    }
}
