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

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ejb3.component.EJBUtilities;
import org.jboss.as.ejb3.deployment.processors.AccessTimeoutAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.ApplicationExceptionAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.AroundTimeoutAnnotationParsingProcessor;
import org.jboss.as.ejb3.deployment.processors.AsynchronousAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.BusinessViewAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.ConcurrencyManagementAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.DeclareRolesProcessor;
import org.jboss.as.ejb3.deployment.processors.DenyAllProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbCleanUpProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbContextJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbDependencyDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbDependsOnAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbInjectionResolutionProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJarConfigurationProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJarParsingDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJndiBindingsDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbRefProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbResourceInjectionAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.ImplicitLocalViewProcessor;
import org.jboss.as.ejb3.deployment.processors.LockAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.MessageDrivenComponentDescriptionFactory;
import org.jboss.as.ejb3.deployment.processors.MethodPermissionDDProcessor;
import org.jboss.as.ejb3.deployment.processors.PermitAllProcessor;
import org.jboss.as.ejb3.deployment.processors.RemoveAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.ResourceAdapterAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.RolesAllowedProcessor;
import org.jboss.as.ejb3.deployment.processors.RunAsProcessor;
import org.jboss.as.ejb3.deployment.processors.SecurityDomainProcessor;
import org.jboss.as.ejb3.deployment.processors.SessionBeanComponentDescriptionFactory;
import org.jboss.as.ejb3.deployment.processors.SessionSynchronizationProcessor;
import org.jboss.as.ejb3.deployment.processors.StartupAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.StatefulTimeoutAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.TimeoutAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.TimerServiceDeploymentProcessor;
import org.jboss.as.ejb3.deployment.processors.TimerServiceJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.TransactionAttributeAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.TransactionManagementAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.AssemblyDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.DeploymentDescriptorInterceptorBindingsProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.DeploymentDescriptorMethodProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.EjbConcurrencyProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.ExcludeListDDProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.InterceptorClassDeploymentDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.RemoveMethodDeploymentDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SecurityIdentityDDProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SecurityRoleRefDDProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SessionBeanXmlDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.TimeoutMethodDeploymentDescriptorProcessor;
import org.jboss.as.ejb3.timerservice.TimerServiceFactoryService;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.as.txn.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import java.util.List;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CORE_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.THREAD_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_DATA_STORE_LOCATION;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_SERVICE;

/**
 * @author Emanuel Muckenhuber
 */
class EJB3SubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final EJB3SubsystemAdd INSTANCE = new EJB3SubsystemAdd();

    private EJB3SubsystemAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        final ModelNode timerService = operation.get(EJB3SubsystemModel.TIMER_SERVICE);
        if (timerService.isDefined()) {
            model.get(TIMER_SERVICE).set(timerService.clone());
        }

    }

    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {

                final ModelNode timerServiceModel = model.get(TIMER_SERVICE);

                boolean timerServiceEnabled = false;

                if (timerServiceModel.isDefined()) {

                    timerServiceEnabled = true;

                    final ModelNode timerDataSource = timerServiceModel.get(TIMER_DATA_STORE_LOCATION);
                    final ModelNode pathNode = timerDataSource.get(PATH);
                    final String path = pathNode.isDefined() ? pathNode.asString() : null;
                    final ModelNode relativeToNode = timerDataSource.get(RELATIVE_TO);
                    final String relativeTo = relativeToNode.isDefined() ? relativeToNode.asString() : null;

                    //install the ejb timer service data store path service
                    if (path != null) {
                        if (relativeTo != null) {
                            RelativePathService.addService(TimerServiceFactoryService.PATH_SERVICE_NAME, path, relativeTo, context.getServiceTarget());
                        } else {
                            AbsolutePathService.addService(TimerServiceFactoryService.PATH_SERVICE_NAME, path, context.getServiceTarget());
                        }
                    }

                    final ModelNode coreThreads = timerServiceModel.get(THREAD_POOL, CORE_THREADS);
                    int threadCount = coreThreads.isDefined() ? coreThreads.asInt() : Runtime.getRuntime().availableProcessors();

                    //we only add the timer service DUP's when the timer service in enabled in XML
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_AROUNDTIMEOUT_ANNOTATION, new AroundTimeoutAnnotationParsingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_TIMER_SERVICE, new TimerServiceDeploymentProcessor(threadCount, timerServiceEnabled));
                }

                //we still parse these annotations even if the timer service is not enabled
                //so we can log a warning about any @Schedule annotations we find
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_TIMEOUT_ANNOTATION, new TimeoutAnnotationProcessor(timerServiceEnabled));

                // add the metadata parser deployment processor
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DEPLOYMENT, new EjbJarParsingDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_SESSION_BEAN_CREATE_COMPONENT_DESCRIPTIONS, new SessionBeanComponentDescriptionFactory());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MDB_CREATE_COMPONENT_DESCRIPTIONS, new MessageDrivenComponentDescriptionFactory());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SESSION_BEAN_DD, new SessionBeanXmlDescriptorProcessor());
                //processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_ANNOTATION, new EjbAnnotationProcessor());
                //processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MESSAGE_DRIVEN_ANNOTATION, new MessageDrivenAnnotationProcessor());
                // Process @DependsOn after the @Singletons have been registered.
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CONTEXT_BINDING, new EjbContextJndiBindingProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_TIMERSERVICE_BINDING, new TimerServiceJndiBindingProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_TRANSACTION_MANAGEMENT, new TransactionManagementAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_INJECTION_ANNOTATION, new EjbResourceInjectionAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_STARTUP_ANNOTATION, new StartupAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_CONCURRENCY_MANAGEMENT_ANNOTATION, new ConcurrencyManagementAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_LOCK_ANNOTATION, new LockAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DECLARE_ROLES_ANNOTATION, new DeclareRolesProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_RUN_AS_ANNOTATION, new RunAsProcessor());
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

                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_EJB, new EjbDependencyDeploymentUnitProcessor());

                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_BUSINESS_VIEW_ANNOTATION, new BusinessViewAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_IMPLICIT_NO_INTERFACE_VIEW, new ImplicitLocalViewProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_JNDI_BINDINGS, new EjbJndiBindingsDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_MODULE_CONFIGURATION, new EjbJarConfigurationProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_INTERCEPTORS, new DeploymentDescriptorInterceptorBindingsProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_CONCURRENCY, new EjbConcurrencyProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_METHOD_RESOLUTION, new DeploymentDescriptorMethodProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_REMOVE_METHOD, new RemoveMethodDeploymentDescriptorProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_TIMEOUT_METHOD, new TimeoutMethodDeploymentDescriptorProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_DENY_ALL_ANNOTATION, new DenyAllProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_ROLES_ALLOWED_ANNOTATION, new RolesAllowedProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_PERMIT_ALL_ANNOTATION, new PermitAllProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_EXCLUDE_LIST_DD, new ExcludeListDDProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_METHOD_PERMISSION_DD, new MethodPermissionDDProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_REF, new EjbRefProcessor());

                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_RESOLVE_EJB_INJECTIONS, new EjbInjectionResolutionProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_DEPENDS_ON_ANNOTATION, new EjbDependsOnAnnotationProcessor());

                processorTarget.addDeploymentProcessor(Phase.CLEANUP, Phase.CLEANUP_EJB, new EjbCleanUpProcessor());


                // add the real deployment processor
                // TODO: add the proper deployment processors
                // processorTarget.addDeploymentProcessor(processor, priority);
            }
        }, OperationContext.Stage.RUNTIME);

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final EJBUtilities utilities = new EJBUtilities();
        newControllers.add(serviceTarget.addService(EJBUtilities.SERVICE_NAME, utilities)
                .addDependency(ConnectorServices.RA_REPOSISTORY_SERVICE, ResourceAdapterRepository.class, utilities.getResourceAdapterRepositoryInjector())
                .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class, utilities.getMdrInjector())
                .addDependency(SimpleSecurityManagerService.SERVICE_NAME, SimpleSecurityManager.class, utilities.getSecurityManagerInjector())
                .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, utilities.getTransactionManagerInjector())
                .addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, utilities.getTransactionSynchronizationRegistryInjector())
                .addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION, UserTransaction.class, utilities.getUserTransactionInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());

    }

}
