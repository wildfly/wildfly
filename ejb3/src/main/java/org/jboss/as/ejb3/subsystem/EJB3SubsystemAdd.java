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

package org.jboss.as.ejb3.subsystem;

import java.util.List;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ejb3.component.EJBUtilities;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.processors.ApplicationExceptionAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.BusinessViewAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.DeploymentRepositoryProcessor;
import org.jboss.as.ejb3.deployment.processors.EJBClientDescriptorMetaDataProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbCleanUpProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbClientContextParsingProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbClientContextSetupProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbContextJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbDependencyDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbInjectionResolutionProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJarParsingDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJndiBindingsDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbManagementDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbRefProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbResourceInjectionAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.IIOPJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.ImplicitLocalViewProcessor;
import org.jboss.as.ejb3.deployment.processors.MessageDrivenComponentDescriptionFactory;
import org.jboss.as.ejb3.deployment.processors.SessionBeanComponentDescriptionFactory;
import org.jboss.as.ejb3.deployment.processors.SessionBeanHomeProcessor;
import org.jboss.as.ejb3.deployment.processors.TimerServiceJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.annotation.EjbAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.AssemblyDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.DeploymentDescriptorInterceptorBindingsProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.DeploymentDescriptorMethodProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.InterceptorClassDeploymentDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SecurityRoleRefDDProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SessionBeanXmlDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.entity.EntityBeanComponentDescriptionFactory;
import org.jboss.as.ejb3.deployment.processors.merging.ApplicationExceptionMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.ConcurrencyManagementMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.DeclareRolesMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.EjbConcurrencyMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.EjbDependsOnMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.HomeViewMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.InitMethodMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.RemoveMethodMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.ResourceAdaptorMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.RunAsMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.SecurityDomainMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.SessionBeanMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.SecurityRolesMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.SessionSynchronizationMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.StartupMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.StatefulTimeoutMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.TransactionAttributeMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.TransactionManagementMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.security.JaccEjbDeploymentProcessor;
import org.jboss.as.ejb3.iiop.POARegistry;
import org.jboss.as.ejb3.iiop.RemoteObjectSubstitutionService;
import org.jboss.as.ejb3.iiop.stub.DynamicStubFactoryFactory;
import org.jboss.as.ejb3.remote.DefaultEjbClientContextService;
import org.jboss.as.ejb3.remote.LocalEjbReceiver;
import org.jboss.as.ejb3.remote.TCCLBasedEJBClientContextSelector;
import org.jboss.as.jacorb.rmi.DelegatingStubFactoryFactory;
import org.jboss.as.jacorb.service.CorbaPOAService;
import org.jboss.as.naming.InitialContext;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.com.sun.corba.se.impl.javax.rmi.RemoteObjectSubstitutionManager;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.naming.ejb.EjbNamingContextSetup;
import org.jboss.ejb.client.naming.ejb.ejbURLContextFactory;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.omg.PortableServer.POA;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.APPCLIENT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT;

/**
 * Add operation handler for the EJB3 subsystem.
 *
 * @author Emanuel Muckenhuber
 */
class EJB3SubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final EJB3SubsystemAdd INSTANCE = new EJB3SubsystemAdd();

    private EJB3SubsystemAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        if (operation.hasDefined(APPCLIENT)) {
            model.get(APPCLIENT).set(operation.get(APPCLIENT));
        }
        model.get(DEFAULT_MDB_INSTANCE_POOL).set(operation.get(DEFAULT_MDB_INSTANCE_POOL));
        model.get(DEFAULT_SLSB_INSTANCE_POOL).set(operation.get(DEFAULT_SLSB_INSTANCE_POOL));
        model.get(DEFAULT_RESOURCE_ADAPTER_NAME).set(operation.get(DEFAULT_RESOURCE_ADAPTER_NAME));
        model.get(DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT).set(operation.get(DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT));
        model.get(DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT).set(operation.get(DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT));
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        //setup IIOP related stuff
        //This goes here rather than in EJB3IIOPAdd as it affects the server when it is acting as an iiop client
        //setup our dynamic stub factory
        DelegatingStubFactoryFactory.setOverridenDynamicFactory(new DynamicStubFactoryFactory());

        //setup the substitution service, that translates between ejb proxies and IIOP stubs
        final RemoteObjectSubstitutionService substitutionService = new RemoteObjectSubstitutionService();
        newControllers.add(context.getServiceTarget().addService(RemoteObjectSubstitutionService.SERVICE_NAME, substitutionService)
                .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, substitutionService.getDeploymentRepositoryInjectedValue())
                .install());

        RemoteObjectSubstitutionManager.setRemoteObjectSubstitution(substitutionService);


        //setup ejb: namespace
        EjbNamingContextSetup.setupEjbNamespace();
        //TODO: this is a bit of a hack
        InitialContext.addUrlContextFactory("ejb", new ejbURLContextFactory());
        final boolean appclient = model.hasDefined(APPCLIENT) && model.get(APPCLIENT).asBoolean();

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {

                //DUP's that are used even for app client deployments
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DEPLOYMENT, new EjbJarParsingDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_SESSION_BEAN_CREATE_COMPONENT_DESCRIPTIONS, new SessionBeanComponentDescriptionFactory(appclient));
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SESSION_BEAN_DD, new SessionBeanXmlDescriptorProcessor(appclient));
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_ANNOTATION_EJB, new EjbAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_INJECTION_ANNOTATION, new EjbResourceInjectionAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CLIENT_METADATA, new EJBClientDescriptorMetaDataProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_ENTITY_BEAN_CREATE_COMPONENT_DESCRIPTIONS, new EntityBeanComponentDescriptionFactory(appclient));
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ASSEMBLY_DESC_DD, new AssemblyDescriptorProcessor());

                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_EJB, new EjbDependencyDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_HOME_MERGE, new HomeViewMergingProcessor(appclient));
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_REF, new EjbRefProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_CLIENT_CONTEXT_SETUP, new EjbClientContextSetupProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_BUSINESS_VIEW_ANNOTATION, new BusinessViewAnnotationProcessor(appclient));
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_ORB_BIND, new IIOPJndiBindingProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_JNDI_BINDINGS, new EjbJndiBindingsDeploymentUnitProcessor(appclient));

                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_RESOLVE_EJB_INJECTIONS, new EjbInjectionResolutionProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EJB_JACC_PROCESSING, new JaccEjbDeploymentProcessor());

                processorTarget.addDeploymentProcessor(Phase.CLEANUP, Phase.CLEANUP_EJB, new EjbCleanUpProcessor());

                if (!appclient) {
                    // add the metadata parser deployment processor

                    // Process @DependsOn after the @Singletons have been registered.
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CONTEXT_BINDING, new EjbContextJndiBindingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_TIMERSERVICE_BINDING, new TimerServiceJndiBindingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_APPLICATION_EXCEPTION_ANNOTATION, new ApplicationExceptionAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DD_INTERCEPTORS, new InterceptorClassDeploymentDescriptorProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SECURITY_ROLE_REF_DD, new SecurityRoleRefDDProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_REMOTE_CLIENT_CONTEXT, new EjbClientContextParsingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MDB_CREATE_COMPONENT_DESCRIPTIONS, new MessageDrivenComponentDescriptionFactory());

                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_IMPLICIT_NO_INTERFACE_VIEW, new ImplicitLocalViewProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_APPLICATION_EXCEPTIONS, new ApplicationExceptionMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_INTERCEPTORS, new DeploymentDescriptorInterceptorBindingsProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_METHOD_RESOLUTION, new DeploymentDescriptorMethodProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_TRANSACTION_MANAGEMENT, new TransactionManagementMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_CONCURRENCY_MANAGEMENT_MERGE, new ConcurrencyManagementMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_CONCURRENCY_MERGE, new EjbConcurrencyMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_TX_ATTR_MERGE, new TransactionAttributeMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_RUN_AS_MERGE, new RunAsMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_RESOURCE_ADAPTER_MERGE, new ResourceAdaptorMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_REMOVE_METHOD, new RemoveMethodMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_STARTUP_MERGE, new StartupMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_SECURITY_DOMAIN, new SecurityDomainMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_ROLES, new DeclareRolesMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_METHOD_PERMISSIONS, new MethodPermissionsMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_STATEFUL_TIMEOUT, new StatefulTimeoutMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_SESSION_SYNCHRONIZATION, new SessionSynchronizationMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_INIT_METHOD, new InitMethodMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_SESSION_BEAN, new SessionBeanMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_SECURITY_PRINCIPAL_ROLE_MAPPING_MERGE, new SecurityRolesMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_LOCAL_HOME, new SessionBeanHomeProcessor());

                    processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_DEPENDS_ON_ANNOTATION, new EjbDependsOnMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_DEPLOYMENT_REPOSITORY, new DeploymentRepositoryProcessor());
                    processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EJB_MANAGEMENT_RESOURCES, new EjbManagementDeploymentUnitProcessor());

                }

            }
        }, OperationContext.Stage.RUNTIME);

        if (model.hasDefined(DEFAULT_MDB_INSTANCE_POOL)) {
            EJB3SubsystemDefaultPoolWriteHandler.MDB_POOL.updatePoolService(context, model, newControllers);
        }

        if (model.hasDefined(DEFAULT_SLSB_INSTANCE_POOL)) {
            EJB3SubsystemDefaultPoolWriteHandler.SLSB_POOL.updatePoolService(context, model, newControllers);
        }

        if (model.hasDefined(DEFAULT_RESOURCE_ADAPTER_NAME)) {
            DefaultResourceAdapterWriteHandler.INSTANCE.updateDefaultAdapterService(context, model, newControllers);
        }

        if (model.hasDefined(DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT)) {
            DefaultSingletonBeanAccessTimeoutWriteHandler.INSTANCE.updateOrCreateDefaultSingletonBeanAccessTimeoutService(context, model, newControllers);
        }

        if (model.hasDefined(DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT)) {
            DefaultStatefulBeanAccessTimeoutWriteHandler.INSTANCE.updateOrCreateDefaultStatefulBeanAccessTimeoutService(context, model, newControllers);
        }

        final ServiceTarget serviceTarget = context.getServiceTarget();

        newControllers.add(context.getServiceTarget().addService(DeploymentRepository.SERVICE_NAME, new DeploymentRepository()).install());

        addRemoteInvocationServices(context, newControllers, appclient);

        if (!appclient) {
            final EJBUtilities utilities = new EJBUtilities();
            newControllers.add(serviceTarget.addService(EJBUtilities.SERVICE_NAME, utilities)
                    .addDependency(ConnectorServices.RA_REPOSISTORY_SERVICE, ResourceAdapterRepository.class, utilities.getResourceAdapterRepositoryInjector())
                    .addDependency(SimpleSecurityManagerService.SERVICE_NAME, SimpleSecurityManager.class, utilities.getSecurityManagerInjector())
                    .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, utilities.getTransactionManagerInjector())
                    .addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, utilities.getTransactionSynchronizationRegistryInjector())
                    .addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION, UserTransaction.class, utilities.getUserTransactionInjector())
                    .addListener(verificationHandler)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install());


            // create the POA Registry use by iiop
            final POARegistry poaRegistry = new POARegistry();
            newControllers.add(context.getServiceTarget().addService(POARegistry.SERVICE_NAME, poaRegistry)
                    .addDependency(CorbaPOAService.ROOT_SERVICE_NAME, POA.class, poaRegistry.getRootPOA())
                    .setInitialMode(ServiceController.Mode.PASSIVE)
                    .addListener(verificationHandler)
                    .install());
        }
    }

    private void addRemoteInvocationServices(final OperationContext context, final List<ServiceController<?>> newControllers, final boolean appclient) {

        // Add the tccl based client context selector
        final TCCLBasedEJBClientContextSelector tcclBasedClientContextSelector = new TCCLBasedEJBClientContextSelector();
        context.getServiceTarget().addService(tcclBasedClientContextSelector.TCCL_BASED_EJB_CLIENT_CONTEXT_SELECTOR_SERVICE_NAME,
                tcclBasedClientContextSelector).install();

        //add the default EjbClientContext
        //TODO: This should be managed
        final DefaultEjbClientContextService clientContextService = new DefaultEjbClientContextService();
        final ServiceBuilder<EJBClientContext> clientContextServiceBuilder = context.getServiceTarget().addService(DefaultEjbClientContextService.DEFAULT_SERVICE_NAME,
                clientContextService).addDependency(TCCLBasedEJBClientContextSelector.TCCL_BASED_EJB_CLIENT_CONTEXT_SELECTOR_SERVICE_NAME,
                TCCLBasedEJBClientContextSelector.class, clientContextService.getTCCLBasedEJBClientContextSelectorInjector());

        if (!appclient) {
            // get the node name
            final String nodeName = SecurityActions.getSystemProperty(ServerEnvironment.NODE_NAME);
            //the default spec compliant EJB reciever
            final LocalEjbReceiver byValueLocalEjbReceiver = new LocalEjbReceiver(nodeName, false);
            newControllers.add(context.getServiceTarget().addService(LocalEjbReceiver.BY_VALUE_SERVICE_NAME, byValueLocalEjbReceiver)
                    .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, byValueLocalEjbReceiver.getDeploymentRepository())
                    .install());

            //the receiver for invocations that allow pass by reference
            final LocalEjbReceiver byReferenceLocalEjbReceiver = new LocalEjbReceiver(nodeName, true);
            newControllers.add(context.getServiceTarget().addService(LocalEjbReceiver.BY_REFERENCE_SERVICE_NAME, byReferenceLocalEjbReceiver)
                    .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, byReferenceLocalEjbReceiver.getDeploymentRepository())
                    .install());

            clientContextService.addReceiver(clientContextServiceBuilder, LocalEjbReceiver.BY_VALUE_SERVICE_NAME);
        }

        newControllers.add(clientContextServiceBuilder.install());
    }

}
