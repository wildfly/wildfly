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

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SFSB_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_CAPABILITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.EJB_CAPABILITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.EJB_CLIENT_CONFIGURATOR;

import java.net.URI;
import java.util.function.Supplier;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.clustering.SingletonBarrierService;
import org.jboss.as.ejb3.component.EJBUtilities;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.processors.AnnotatedEJBComponentDescriptionDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.ApplicationExceptionAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.BusinessViewAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.CacheDependenciesProcessor;
import org.jboss.as.ejb3.deployment.processors.DeploymentRepositoryProcessor;
import org.jboss.as.ejb3.deployment.processors.DiscoveryRegistrationProcessor;
import org.jboss.as.ejb3.deployment.processors.EJBClientDescriptorMetaDataProcessor;
import org.jboss.as.ejb3.deployment.processors.EJBComponentSuspendDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EJBDefaultSecurityDomainProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbCleanUpProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbClientContextSetupProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbContextJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbDefaultDistinctNameProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbDependencyDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJarJBossAllParser;
import org.jboss.as.ejb3.deployment.processors.EjbJarParsingDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbJndiBindingsDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbManagementDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbRefProcessor;
import org.jboss.as.ejb3.deployment.processors.EjbResourceInjectionAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.IIOPJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.ImplicitLocalViewProcessor;
import org.jboss.as.ejb3.deployment.processors.MdbDeliveryDependenciesProcessor;
import org.jboss.as.ejb3.deployment.processors.PassivationAnnotationParsingProcessor;
import org.jboss.as.ejb3.deployment.processors.SessionBeanHomeProcessor;
import org.jboss.as.ejb3.deployment.processors.StartupAwaitDeploymentUnitProcessor;
import org.jboss.as.ejb3.deployment.processors.TimerServiceJndiBindingProcessor;
import org.jboss.as.ejb3.deployment.processors.annotation.EjbAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.AssemblyDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.ContainerInterceptorBindingsDDProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.DeploymentDescriptorInterceptorBindingsProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.DeploymentDescriptorMethodProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.InterceptorClassDeploymentDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SecurityRoleRefDDProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.SessionBeanXmlDescriptorProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.ApplicationExceptionMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.CacheMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.ClusteredSingletonMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.ConcurrencyManagementMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.DeclareRolesMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.EjbConcurrencyMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.EjbDependsOnMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.HomeViewMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.InitMethodMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.MdbDeliveryMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.MessageDrivenBeanPoolMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.MissingMethodPermissionsDenyAccessMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.RemoveMethodMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.ResourceAdaptorMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.RunAsMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.SecurityDomainMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.SecurityRolesMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.SessionBeanMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.SessionSynchronizationMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.StartupMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.StatefulTimeoutMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.StatelessSessionBeanPoolMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.TransactionAttributeMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.TransactionManagementMergingProcessor;
import org.jboss.as.ejb3.deployment.processors.security.JaccEjbDeploymentProcessor;
import org.jboss.as.ejb3.iiop.POARegistry;
import org.jboss.as.ejb3.iiop.RemoteObjectSubstitutionService;
import org.jboss.as.ejb3.iiop.stub.DynamicStubFactoryFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.ClientMappingsRegistryServiceConfigurator;
import org.jboss.as.ejb3.remote.EJBClientContextService;
import org.jboss.as.ejb3.remote.LocalTransportProvider;
import org.jboss.as.ejb3.remote.http.EJB3RemoteHTTPService;
import org.jboss.as.ejb3.suspend.EJBSuspendHandlerService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.as.txn.service.UserTransactionAccessControlService;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.javax.rmi.RemoteObjectSubstitutionManager;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.omg.PortableServer.POA;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;
import org.wildfly.clustering.singleton.service.SingletonPolicy;
import org.wildfly.iiop.openjdk.rmi.DelegatingStubFactoryFactory;
import org.wildfly.iiop.openjdk.service.CorbaPOAService;
import org.wildfly.transaction.client.LocalTransactionContext;

import io.undertow.server.handlers.PathHandler;

/**
 * Add operation handler for the EJB3 subsystem.
 *
 * @author Emanuel Muckenhuber
 */
class EJB3SubsystemAdd extends AbstractBoottimeAddStepHandler {

    private final EJBDefaultSecurityDomainProcessor defaultSecurityDomainDeploymentProcessor;
    private final MissingMethodPermissionsDenyAccessMergingProcessor missingMethodPermissionsDenyAccessMergingProcessor;
    private static final String UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME = "org.wildfly.undertow.http-invoker";

    private static final String REMOTING_ENDPOINT_CAPABILITY = "org.wildfly.remoting.endpoint";

    EJB3SubsystemAdd(final EJBDefaultSecurityDomainProcessor defaultSecurityDomainDeploymentProcessor, final MissingMethodPermissionsDenyAccessMergingProcessor missingMethodPermissionsDenyAccessMergingProcessor) {
        this.defaultSecurityDomainDeploymentProcessor = defaultSecurityDomainDeploymentProcessor;
        this.missingMethodPermissionsDenyAccessMergingProcessor = missingMethodPermissionsDenyAccessMergingProcessor;
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.recordCapabilitiesAndRequirements(context, operation, resource);
        // TODO: delete these once optional requirements no longer require the existence of a capability
        context.registerCapability(CLUSTERED_SINGLETON_CAPABILITY);
        context.registerCapability(EJB_CLIENT_CONFIGURATOR);
        context.registerCapability(EJB_CAPABILITY);
    }

    @Override
    protected void populateModel(final OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        ModelNode model = resource.getModel();
        for (AttributeDefinition attr : EJB3SubsystemRootResourceDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }

        // WFLY-5520 deal with legacy default-clustered-sfsb-cache
        ModelNode defClustered = DEFAULT_CLUSTERED_SFSB_CACHE.validateOperation(operation);
        if (defClustered.isDefined())  {
            boolean setDefaultSfsbCache = true;
            // Assume this is a legacy script and try and adapt the params to the new attributes
            if (model.hasDefined(DEFAULT_SFSB_CACHE)) {
                if (model.hasDefined(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE)) {
                    // All 3 params were defined. This is only ok if default-clustered-sfsb-cache and default-sfsb-cache
                    // are the same, meaning default-clustered-sfsb-cache is redundant
                    if (!defClustered.equals(model.get(DEFAULT_SFSB_CACHE))) {
                        // No good. Log or fail
                        if(context.getRunningMode() == RunningMode.ADMIN_ONLY) {
                            EjbLogger.ROOT_LOGGER.logInconsistentAttributeNotSupported(DEFAULT_CLUSTERED_SFSB_CACHE.getName(), DEFAULT_SFSB_CACHE);
                            setDefaultSfsbCache = false; // don't overwrite default-sfsb-cache
                        } else {
                            throw EjbLogger.ROOT_LOGGER.inconsistentAttributeNotSupported(DEFAULT_CLUSTERED_SFSB_CACHE.getName(), DEFAULT_SFSB_CACHE);
                        }
                    }
                } else {
                    // The old attributes were defined; new one wasn't so, move the old default-sfsb-cache to default-passivation-disabled
                    model.get(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE).set(model.get(DEFAULT_SFSB_CACHE));
                }
            }
            if (setDefaultSfsbCache) {
                model.get(DEFAULT_SFSB_CACHE).set(defClustered);
                EjbLogger.ROOT_LOGGER.remappingCacheAttributes(context.getCurrentAddress().toCLIStyleString(), defClustered, model.get(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE));
            }
        }
    }

    @Override
    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {

        // Install the server association service
        final AssociationService associationService = new AssociationService();
        final ServiceBuilder<AssociationService> associationServiceBuilder = context.getServiceTarget().addService(AssociationService.SERVICE_NAME, associationService);
        associationServiceBuilder.addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, associationService.getDeploymentRepositoryInjector())
                .addDependency(SuspendController.SERVICE_NAME, SuspendController.class, associationService.getSuspendControllerInjector())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, associationService.getServerEnvironmentServiceInjector())
                .setInitialMode(ServiceController.Mode.LAZY);

        if (context.readResource(PathAddress.EMPTY_ADDRESS, false).hasChild(EJB3SubsystemModel.REMOTE_SERVICE_PATH)) {
            associationServiceBuilder.addDependency(ClientMappingsRegistryServiceConfigurator.SERVICE_NAME, Registry.class, associationService.getClientMappingsRegistryInjector());
        }
        associationServiceBuilder.install();

        //setup IIOP related stuff
        //This goes here rather than in EJB3IIOPAdd as it affects the server when it is acting as an iiop client
        //setup our dynamic stub factory
        DelegatingStubFactoryFactory.setOverriddenDynamicFactory(new DynamicStubFactoryFactory());

        //setup the substitution service, that translates between ejb proxies and IIOP stubs
        final RemoteObjectSubstitutionService substitutionService = new RemoteObjectSubstitutionService();
        context.getServiceTarget().addService(RemoteObjectSubstitutionService.SERVICE_NAME, substitutionService)
                .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, substitutionService.getDeploymentRepositoryInjectedValue())
                .install();

        // register EJB context selector

        RemoteObjectSubstitutionManager.setRemoteObjectSubstitution(substitutionService);

        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;

        final ModelNode defaultDistinctName = EJB3SubsystemRootResourceDefinition.DEFAULT_DISTINCT_NAME.resolveModelAttribute(context, model);
        final DefaultDistinctNameService defaultDistinctNameService = new DefaultDistinctNameService(defaultDistinctName.isDefined() ? defaultDistinctName.asString() : null);
        context.getServiceTarget().addService(DefaultDistinctNameService.SERVICE_NAME, defaultDistinctNameService).install();
        final ModelNode ejbNameRegex = EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX.resolveModelAttribute(context, model);
        final EjbNameRegexService ejbNameRegexService = new EjbNameRegexService(ejbNameRegex.isDefined() ? ejbNameRegex.asBoolean() : false);
        context.getServiceTarget().addService(EjbNameRegexService.SERVICE_NAME, ejbNameRegexService).install();

        // set the default security domain name in the deployment unit processor, configured at the subsystem level
        final ModelNode defaultSecurityDomainModelNode = EJB3SubsystemRootResourceDefinition.DEFAULT_SECURITY_DOMAIN.resolveModelAttribute(context, model);
        final String defaultSecurityDomain = defaultSecurityDomainModelNode.isDefined() ? defaultSecurityDomainModelNode.asString() : null;
        this.defaultSecurityDomainDeploymentProcessor.setDefaultSecurityDomainName(defaultSecurityDomain);

        // set the default security domain name in the deployment unit processor, configured at the subsytem level
        final ModelNode defaultMissingMethod = EJB3SubsystemRootResourceDefinition.DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS.resolveModelAttribute(context, model);
        final boolean defaultMissingMethodValue = defaultMissingMethod.asBoolean();
        this.missingMethodPermissionsDenyAccessMergingProcessor.setDenyAccessByDefault(defaultMissingMethodValue);

        final boolean defaultMdbPoolAvailable = model.hasDefined(DEFAULT_MDB_INSTANCE_POOL);
        final boolean defaultSlsbPoolAvailable = model.hasDefined(DEFAULT_SLSB_INSTANCE_POOL);

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {

                //DUP's that are used even for app client deployments
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_EJB, new JBossAllXmlParserRegisteringProcessor<EjbJarMetaData>(EjbJarJBossAllParser.ROOT_ELEMENT, EjbJarJBossAllParser.ATTACHMENT_KEY, new EjbJarJBossAllParser()));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_DEFAULT_DISTINCT_NAME, new EjbDefaultDistinctNameProcessor(defaultDistinctNameService));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_CONTEXT_BINDING, new EjbContextJndiBindingProcessor());
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_DEPLOYMENT, new EjbJarParsingDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_CREATE_COMPONENT_DESCRIPTIONS, new AnnotatedEJBComponentDescriptionDeploymentUnitProcessor(appclient, defaultMdbPoolAvailable, defaultSlsbPoolAvailable));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_SESSION_BEAN_DD, new SessionBeanXmlDescriptorProcessor(appclient));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_ANNOTATION_EJB, new EjbAnnotationProcessor());
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_INJECTION_ANNOTATION, new EjbResourceInjectionAnnotationProcessor(appclient));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_ASSEMBLY_DESC_DD, new AssemblyDescriptorProcessor());

                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_EJB, new EjbDependencyDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_HOME_MERGE, new HomeViewMergingProcessor(appclient));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_REF, new EjbRefProcessor(appclient));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_BUSINESS_VIEW_ANNOTATION, new BusinessViewAnnotationProcessor(appclient));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_ORB_BIND, new IIOPJndiBindingProcessor());
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_JNDI_BINDINGS, new EjbJndiBindingsDeploymentUnitProcessor(appclient));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_CLIENT_METADATA, new EJBClientDescriptorMetaDataProcessor(appclient));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_DISCOVERY, new DiscoveryRegistrationProcessor(appclient));
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_DEFAULT_SECURITY_DOMAIN, EJB3SubsystemAdd.this.defaultSecurityDomainDeploymentProcessor);
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EE_COMPONENT_SUSPEND, new EJBComponentSuspendDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EE_COMPONENT_SUSPEND + 1, new EjbClientContextSetupProcessor()); //TODO: real phase numbers
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EE_COMPONENT_SUSPEND + 2, new StartupAwaitDeploymentUnitProcessor());

                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_EJB_JACC_PROCESSING, new JaccEjbDeploymentProcessor());

                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.CLEANUP, Phase.CLEANUP_EJB, new EjbCleanUpProcessor());

                if (!appclient) {
                    // add the metadata parser deployment processor

                    // Process @DependsOn after the @Singletons have been registered.
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_TIMERSERVICE_BINDING, new TimerServiceJndiBindingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_APPLICATION_EXCEPTION_ANNOTATION, new ApplicationExceptionAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_DD_INTERCEPTORS, new InterceptorClassDeploymentDescriptorProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EJB_SECURITY_ROLE_REF_DD, new SecurityRoleRefDDProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_PASSIVATION_ANNOTATION, new PassivationAnnotationParsingProcessor());

                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_IMPLICIT_NO_INTERFACE_VIEW, new ImplicitLocalViewProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_APPLICATION_EXCEPTIONS, new ApplicationExceptionMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_INTERCEPTORS, new DeploymentDescriptorInterceptorBindingsProcessor(ejbNameRegexService));
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_DD_METHOD_RESOLUTION, new DeploymentDescriptorMethodProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_TRANSACTION_MANAGEMENT, new TransactionManagementMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_CONCURRENCY_MANAGEMENT_MERGE, new ConcurrencyManagementMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_CONCURRENCY_MERGE, new EjbConcurrencyMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_TX_ATTR_MERGE, new TransactionAttributeMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_RUN_AS_MERGE, new RunAsMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_RESOURCE_ADAPTER_MERGE, new ResourceAdaptorMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_CLUSTERED, new ClusteredSingletonMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_DELIVERY_ACTIVE_MERGE, new MdbDeliveryMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_REMOVE_METHOD, new RemoveMethodMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_STARTUP_MERGE, new StartupMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_SECURITY_DOMAIN, new SecurityDomainMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_SECURITY_MISSING_METHOD_PERMISSIONS, missingMethodPermissionsDenyAccessMergingProcessor);
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_ROLES, new DeclareRolesMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_METHOD_PERMISSIONS, new MethodPermissionsMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_STATEFUL_TIMEOUT, new StatefulTimeoutMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_SESSION_SYNCHRONIZATION, new SessionSynchronizationMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_INIT_METHOD, new InitMethodMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_SESSION_BEAN, new SessionBeanMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_SECURITY_PRINCIPAL_ROLE_MAPPING_MERGE, new SecurityRolesMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_LOCAL_HOME, new SessionBeanHomeProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_CACHE, new CacheMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_SLSB_POOL_NAME_MERGE, new StatelessSessionBeanPoolMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_MDB_POOL_NAME_MERGE, new MessageDrivenBeanPoolMergingProcessor());
                    // Add the deployment unit processor responsible for processing the user application specific container interceptors configured in jboss-ejb3.xml
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_USER_APP_SPECIFIC_CONTAINER_INTERCEPTORS, new ContainerInterceptorBindingsDDProcessor());

                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DEPENDS_ON_ANNOTATION, new EjbDependsOnMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DEPLOYMENT_REPOSITORY, new DeploymentRepositoryProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_EJB_MANAGEMENT_RESOURCES, new EjbManagementDeploymentUnitProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_CACHE_DEPENDENCIES, new CacheDependenciesProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_EE_MODULE_CONFIG + 1, new MdbDeliveryDependenciesProcessor()); // TODO Phase: replace by  Phase.INSTALL_MDB_DELIVERY_DEPENDENCIES
                }

            }
        }, OperationContext.Stage.RUNTIME);

        //todo maybe needs EJB3SubsystemRootResourceDefinition.DEFAULT_MDB_INSTANCE_POOL.resolveModelAttribute(context,model).isDefined()
        if (model.hasDefined(DEFAULT_MDB_INSTANCE_POOL)) {
            EJB3SubsystemDefaultPoolWriteHandler.MDB_POOL.updatePoolService(context, model);
        }

        if (model.hasDefined(DEFAULT_SLSB_INSTANCE_POOL)) {
            EJB3SubsystemDefaultPoolWriteHandler.SLSB_POOL.updatePoolService(context, model);
        }

        if (model.hasDefined(DEFAULT_ENTITY_BEAN_INSTANCE_POOL)) {
            EJB3SubsystemDefaultPoolWriteHandler.ENTITY_BEAN_POOL.updatePoolService(context, model);
        }

        if (model.hasDefined(DEFAULT_SFSB_CACHE)) {
            EJB3SubsystemDefaultCacheWriteHandler.SFSB_CACHE.updateCacheService(context, model);
        }

        if (model.hasDefined(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE)) {
            EJB3SubsystemDefaultCacheWriteHandler.SFSB_PASSIVATION_DISABLED_CACHE.updateCacheService(context, model);
        }

        if (model.hasDefined(DEFAULT_RESOURCE_ADAPTER_NAME)) {
            DefaultResourceAdapterWriteHandler.INSTANCE.updateDefaultAdapterService(context, model);
        }

        if (model.hasDefined(DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT)) {
            DefaultSingletonBeanAccessTimeoutWriteHandler.INSTANCE.updateOrCreateDefaultSingletonBeanAccessTimeoutService(context, model);
        }

        if (model.hasDefined(DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT)) {
            DefaultStatefulBeanAccessTimeoutWriteHandler.INSTANCE.updateOrCreateDefaultStatefulBeanAccessTimeoutService(context, model);
        }

        if (model.hasDefined(DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING)) {
            EJB3SubsystemDefaultEntityBeanOptimisticLockingWriteHandler.INSTANCE.updateOptimisticLocking(context, model);
        }

        ExceptionLoggingWriteHandler.INSTANCE.updateOrCreateDefaultExceptionLoggingEnabledService(context, model);

        final ServiceTarget serviceTarget = context.getServiceTarget();

        context.getServiceTarget().addService(DeploymentRepository.SERVICE_NAME, new DeploymentRepository()).install();

        addRemoteInvocationServices(context, model, appclient);
        // add clustering service
        addClusteringServices(context, appclient);

        // add user transaction access control service
        final EJB3UserTransactionAccessControlService userTxAccessControlService = new EJB3UserTransactionAccessControlService();
        context.getServiceTarget().addService(EJB3UserTransactionAccessControlService.SERVICE_NAME, userTxAccessControlService)
                .addDependency(UserTransactionAccessControlService.SERVICE_NAME, UserTransactionAccessControlService.class, userTxAccessControlService.getUserTransactionAccessControlServiceInjector())
                .install();

        // add ejb suspend handler service
        boolean enableGracefulShutdown = EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN.resolveModelAttribute(context, model).asBoolean();
        final EJBSuspendHandlerService ejbSuspendHandlerService = new EJBSuspendHandlerService(enableGracefulShutdown);
        context.getServiceTarget().addService(EJBSuspendHandlerService.SERVICE_NAME, ejbSuspendHandlerService)
                .addDependency(SuspendController.SERVICE_NAME, SuspendController.class, ejbSuspendHandlerService.getSuspendControllerInjectedValue())
                .addDependency(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT, LocalTransactionContext.class, ejbSuspendHandlerService.getLocalTransactionContextInjectedValue())
                .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, ejbSuspendHandlerService.getDeploymentRepositoryInjectedValue())
                .install();

        if (!appclient) {
            final EJBUtilities utilities = new EJBUtilities();
            ServiceBuilder<EJBUtilities> ejbUtilsBuilder = serviceTarget.addService(EJBUtilities.SERVICE_NAME, utilities)
                    .addDependency(ConnectorServices.RA_REPOSITORY_SERVICE, ResourceAdapterRepository.class, utilities.getResourceAdapterRepositoryInjector())
                    .setInitialMode(ServiceController.Mode.PASSIVE);
            ejbUtilsBuilder.install();


            // create the POA Registry use by iiop
            final POARegistry poaRegistry = new POARegistry();
            context.getServiceTarget().addService(POARegistry.SERVICE_NAME, poaRegistry)
                    .addDependency(CorbaPOAService.ROOT_SERVICE_NAME, POA.class, poaRegistry.getRootPOA())
                    .setInitialMode(ServiceController.Mode.PASSIVE)
                    .install();

            StatisticsEnabledWriteHandler.INSTANCE.updateToRuntime(context, model);


            if(context.hasOptionalCapability(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME, EJB3SubsystemRootResourceDefinition.EJB_CAPABILITY.getName(), null)) {
                EJB3RemoteHTTPService service = new EJB3RemoteHTTPService();

                context.getServiceTarget().addService(EJB3RemoteHTTPService.SERVICE_NAME, service)
                        .addDependency(context.getCapabilityServiceName(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME, PathHandler.class), PathHandler.class, service.getPathHandlerInjectedValue())
                        .addDependency(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT, LocalTransactionContext.class, service.getLocalTransactionContextInjectedValue())
                        .addDependency(AssociationService.SERVICE_NAME, AssociationService.class, service.getAssociationServiceInjectedValue())
                        .setInitialMode(ServiceController.Mode.PASSIVE)
                        .install();
            }
        }
    }

    private static void addRemoteInvocationServices(final OperationContext context,
                                             final ModelNode ejbSubsystemModel, final boolean appclient) throws OperationFailedException {

        final ServiceTarget serviceTarget = context.getServiceTarget();
        //add the default EjbClientContext

        final EJBClientConfiguratorService clientConfiguratorService = new EJBClientConfiguratorService();
        final ServiceBuilder<EJBClientConfiguratorService> configuratorBuilder = serviceTarget.addService(EJBClientConfiguratorService.SERVICE_NAME, clientConfiguratorService);
        if(context.hasOptionalCapability(REMOTING_ENDPOINT_CAPABILITY, EJB3SubsystemRootResourceDefinition.EJB_CLIENT_CONFIGURATOR.getName(), null)) {
            ServiceName serviceName = context.getCapabilityServiceName(REMOTING_ENDPOINT_CAPABILITY, Endpoint.class);
            configuratorBuilder.addDependency(serviceName, Endpoint.class, clientConfiguratorService.getEndpointInjector());
        }
        configuratorBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();


        //TODO: This should be managed
        final EJBClientContextService clientContextService = new EJBClientContextService(true);
        final ServiceBuilder<EJBClientContextService> clientContextServiceBuilder = context.getServiceTarget().addService(EJBClientContextService.DEFAULT_SERVICE_NAME, clientContextService);

        clientContextServiceBuilder.addDependency(EJBClientConfiguratorService.SERVICE_NAME, EJBClientConfiguratorService.class, clientContextService.getConfiguratorServiceInjector());

        if(appclient) {
            clientContextServiceBuilder.addDependency(EJBClientContextService.APP_CLIENT_URI_SERVICE_NAME, URI.class, clientContextService.getAppClientUri());
            clientContextServiceBuilder.addDependency(EJBClientContextService.APP_CLIENT_EJB_PROPERTIES_SERVICE_NAME, String.class, clientContextService.getAppClientEjbProperties());
        }
        if (!appclient) {
            //the default spec compliant EJB receiver
            final LocalTransportProvider byValueLocalEjbReceiver = new LocalTransportProvider(false);
            ServiceBuilder<LocalTransportProvider> byValueServiceBuilder = serviceTarget.addService(LocalTransportProvider.BY_VALUE_SERVICE_NAME, byValueLocalEjbReceiver)
                    .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, byValueLocalEjbReceiver.getDeploymentRepository())
                    .setInitialMode(ServiceController.Mode.ON_DEMAND);
            byValueServiceBuilder.install();

            //the receiver for invocations that allow pass by reference
            final LocalTransportProvider byReferenceLocalEjbReceiver = new LocalTransportProvider(true);
            ServiceBuilder<LocalTransportProvider> byReferenceServiceBuilder = serviceTarget.addService(LocalTransportProvider.BY_REFERENCE_SERVICE_NAME, byReferenceLocalEjbReceiver)
                    .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, byReferenceLocalEjbReceiver.getDeploymentRepository())
                    .setInitialMode(ServiceController.Mode.ON_DEMAND);
            byReferenceServiceBuilder.install();

            // setup the default local ejb receiver service
            EJBRemoteInvocationPassByValueWriteHandler.INSTANCE.updateDefaultLocalEJBReceiverService(context, ejbSubsystemModel);
            // add the default local ejb receiver to the client context
            clientContextServiceBuilder.addDependency(LocalTransportProvider.DEFAULT_LOCAL_TRANSPORT_PROVIDER_SERVICE_NAME, EJBTransportProvider.class, clientContextService.getLocalProviderInjector());
        }

        // install the default EJB client context service
        clientContextServiceBuilder.install();
    }

    private static void addClusteringServices(final OperationContext context, final boolean appclient) {
        ServiceTarget target = context.getServiceTarget();
        if (appclient) {
            return;
        }
        if (context.hasOptionalCapability(SingletonDefaultRequirement.POLICY.getName(), CLUSTERED_SINGLETON_CAPABILITY.getName(), null)) {
            ServiceBuilder<?> builder = target.addService(SingletonBarrierService.SERVICE_NAME);
            Supplier<SingletonPolicy> policy = builder.requires(context.getCapabilityServiceName(SingletonDefaultRequirement.POLICY.getName(), SingletonDefaultRequirement.POLICY.getType()));
            builder.setInstance(new SingletonBarrierService(policy)).install();
        }
    }
}
