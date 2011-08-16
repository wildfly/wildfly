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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.ejb3.component.EJBUtilities;
import org.jboss.as.ejb3.deployment.processors.AccessTimeoutAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.ApplicationExceptionAnnotationProcessor;
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
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.txn.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import java.util.List;
import java.util.Locale;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.LITE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.TIMER_SERVICE;

/**
 * Add operation handler for the EJB3 subsystem.
 *
 * @author Emanuel Muckenhuber
 */
class EJB3SubsystemAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {

    static final EJB3SubsystemAdd INSTANCE = new EJB3SubsystemAdd();

    private static final Logger logger = Logger.getLogger(EJB3SubsystemAdd.class);

    private EJB3SubsystemAdd() {
        //
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return EJB3SubsystemDescriptions.getSubystemAddDescription(locale);
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(LITE).set(operation.get(LITE));
        model.get(DEFAULT_MDB_INSTANCE_POOL).set(operation.get(DEFAULT_MDB_INSTANCE_POOL));
        model.get(DEFAULT_SLSB_INSTANCE_POOL).set(operation.get(DEFAULT_SLSB_INSTANCE_POOL));
        model.get(DEFAULT_RESOURCE_ADAPTER_NAME).set(operation.get(DEFAULT_RESOURCE_ADAPTER_NAME));
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {

                // we skip timerservice processing if strict webprofile is desired
                // WARNING: This is a bit funky as we are configuring TimeoutAnnotationProcessor in the subsystem
                // root add handler based on configuration that will be done in a later handler, for a child resource
                // (TimerServiceAdd). This can work because if this and TimerServiceAdd are run as part of the same
                // composite op or set of boot operations, this Stage.RUNTIME handler will execute after TimerServiceAdd's
                // Stage.MODEL handler runs. So the model check that's done in the 'if' test below works. But this
                // would fail if TimerServiceAdd were run in a separate set of operations.
                // The reason it's ok is because this and TimerServiceAdd are boot time handlers, so these runtime
                // changes will only happen as a group as part of the set of boot time ops. But it's fragile.
                // TODO look into a way to have TimerServiceAdd toggle the state of the TimeoutAnnotationProcessor we add here.
                boolean timerServiceEnabled = false;
                boolean lite = model.hasDefined(LITE) && model.get(LITE).asBoolean();
                if (!lite && context.readResource(PathAddress.EMPTY_ADDRESS).hasChild(EJB3SubsystemModel.TIMER_SERVICE_PATH)) {
                    timerServiceEnabled = true;
                }

                //we still parse these annotations even if the timer service is not enabled
                //so we can log a warning about any @Schedule annotations we find
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_TIMEOUT_ANNOTATION, new TimeoutAnnotationProcessor(timerServiceEnabled));

                // add the metadata parser deployment processor
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_DEPLOYMENT, new EjbJarParsingDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_SESSION_BEAN_CREATE_COMPONENT_DESCRIPTIONS, new SessionBeanComponentDescriptionFactory());
                // If strict EE webprofile compliance is desired then skip MDB processing
                if (! lite) {
                    logger.debug("Add support for MDB");
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MDB_CREATE_COMPONENT_DESCRIPTIONS, new MessageDrivenComponentDescriptionFactory());
                }
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EJB_SESSION_BEAN_DD, new SessionBeanXmlDescriptorProcessor());
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
            }
        }, OperationContext.Stage.RUNTIME);

        if (model.hasDefined(DEFAULT_MDB_INSTANCE_POOL)) {
            final String poolName = model.get(DEFAULT_MDB_INSTANCE_POOL).asString();
            context.addStep(new SetDefaultMDBPool.DefaultMDBPoolConfigServiceUpdateHandler(poolName), OperationContext.Stage.RUNTIME);
        }

        if (model.hasDefined(DEFAULT_SLSB_INSTANCE_POOL)) {
            final String poolName = model.get(DEFAULT_SLSB_INSTANCE_POOL).asString();
            context.addStep(new SetDefaultSLSBPool.DefaultSLSBPoolConfigServiceUpdateHandler(poolName), OperationContext.Stage.RUNTIME);
        }

        if (model.hasDefined(DEFAULT_RESOURCE_ADAPTER_NAME)) {
            final String raName = model.get(DEFAULT_RESOURCE_ADAPTER_NAME).asString();
            context.addStep(new SetDefaultResourceAdapterName.DefaultResourceAdapterNameUpdateHandler(raName), OperationContext.Stage.RUNTIME);
        }

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
