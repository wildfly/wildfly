/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3RemoteResourceDefinition.CONNECTOR_CAPABILITY_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLIENT_INTERCEPTORS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SFSB_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVER_INTERCEPTORS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_BARRIER;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_CAPABILITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_MDB_POOL_CONFIG_CAPABILITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_SLSB_POOL_CONFIG_CAPABILITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME;
import static org.jboss.as.ejb3.subsystem.StrictMaxPoolResourceDefinition.STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.naming.NamingException;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryService;
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
import org.jboss.as.ejb3.deployment.processors.HibernateValidatorDeploymentUnitProcessor;
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
import org.jboss.as.ejb3.interceptor.server.ClientInterceptorCache;
import org.jboss.as.ejb3.interceptor.server.ServerInterceptorCache;
import org.jboss.as.ejb3.interceptor.server.ServerInterceptorMetaData;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.EJBClientContextService;
import org.jboss.as.ejb3.remote.LocalTransportProvider;
import org.jboss.as.ejb3.security.ApplicationSecurityDomainConfig;
import org.jboss.as.ejb3.suspend.EJBSuspendHandlerService;
import org.jboss.as.network.ProtocolSocketBinding;
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
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.remoting3.Endpoint;
import org.omg.PortableServer.POA;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;
import org.wildfly.common.function.Functions;
import org.wildfly.iiop.openjdk.rmi.DelegatingStubFactoryFactory;
import org.wildfly.iiop.openjdk.service.CorbaPOAService;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.naming.txn.TxnNamingContextFactory;

/**
 * Add operation handler for the EJB3 subsystem.
 *
 * NOTE: References in this file to Enterprise JavaBeans (EJB) refer to the Jakarta Enterprise Beans unless otherwise noted.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class EJB3SubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final String REMOTING_ENDPOINT_CAPABILITY = "org.wildfly.remoting.endpoint";

    private final AtomicReference<String> defaultSecurityDomainName;
    private final Iterable<ApplicationSecurityDomainConfig> knownApplicationSecurityDomains;
    private final Iterable<String> outflowSecurityDomains;
    private final AtomicBoolean denyAccessByDefault;

    EJB3SubsystemAdd(AtomicReference<String> defaultSecurityDomainName, Iterable<ApplicationSecurityDomainConfig> knownApplicationSecurityDomains, Iterable<String> outflowSecurityDomains, AtomicBoolean denyAccessByDefault) {
        this.defaultSecurityDomainName = defaultSecurityDomainName;
        this.knownApplicationSecurityDomains = knownApplicationSecurityDomains;
        this.outflowSecurityDomains = outflowSecurityDomains;
        this.denyAccessByDefault = denyAccessByDefault;
    }

    @Override
    protected void populateModel(final OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.populateModel(context, operation, resource);

        ModelNode model = resource.getModel();
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

    /*
     * Conditional registration of capabilities for default bean instance pools (which may or may not be defined)
     */
    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        ModelNode model = resource.getModel();

        // register the capability we are exporting as well as its capability requirement on the strict-max-pool that supports it
        if (model.hasDefined(DEFAULT_SLSB_INSTANCE_POOL)) {
            context.registerCapability(DEFAULT_SLSB_POOL_CONFIG_CAPABILITY);

            try {
                // need to resolve the attribute value before using it
                String resolvedDefaultSLSBPoolName = context.resolveExpressions(model.get(DEFAULT_SLSB_INSTANCE_POOL)).asString();
                String defaultSLSBPoolRequirementName = RuntimeCapability.buildDynamicCapabilityName(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, resolvedDefaultSLSBPoolName);

                context.registerAdditionalCapabilityRequirement(defaultSLSBPoolRequirementName, DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME, DEFAULT_SLSB_INSTANCE_POOL);
            } catch (OperationFailedException ofe) {
                EjbLogger.ROOT_LOGGER.defaultPoolExpressionCouldNotBeResolved(DEFAULT_SLSB_INSTANCE_POOL, model.get(DEFAULT_SLSB_INSTANCE_POOL).asString());
            }
        }

        if (model.hasDefined(DEFAULT_MDB_INSTANCE_POOL)) {
            context.registerCapability(DEFAULT_MDB_POOL_CONFIG_CAPABILITY);

            try {
                // need to resolve the attribute value before using it
                String resolvedDefaultMDBPoolName = context.resolveExpressions(model.get(DEFAULT_MDB_INSTANCE_POOL)).asString();
                String defaultMDBPoolRequirementName = RuntimeCapability.buildDynamicCapabilityName(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, resolvedDefaultMDBPoolName);

                context.registerAdditionalCapabilityRequirement(defaultMDBPoolRequirementName, DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME, DEFAULT_MDB_INSTANCE_POOL);
            } catch(OperationFailedException ofe) {
                EjbLogger.ROOT_LOGGER.defaultPoolExpressionCouldNotBeResolved(DEFAULT_MDB_INSTANCE_POOL, model.get(DEFAULT_MDB_INSTANCE_POOL).asString());
            }
        }

        super.recordCapabilitiesAndRequirements(context, operation, resource);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        final ModelNode model = resource.getModel();
        // Install the server association service
        final AssociationService associationService = new AssociationService();
        final ServiceName suspendControllerServiceName = context.getCapabilityServiceName("org.wildfly.server.suspend-controller", SuspendController.class);
        final ServiceBuilder<AssociationService> associationServiceBuilder = context.getServiceTarget().addService(AssociationService.SERVICE_NAME, associationService);
        associationServiceBuilder.addDependency(DeploymentRepositoryService.SERVICE_NAME, DeploymentRepository.class, associationService.getDeploymentRepositoryInjector())
                .addDependency(suspendControllerServiceName, SuspendController.class, associationService.getSuspendControllerInjector())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, associationService.getServerEnvironmentServiceInjector());

        if (resource.hasChild(EJB3SubsystemModel.REMOTE_SERVICE_PATH)) {
            ModelNode remoteModel = resource.getChild(EJB3SubsystemModel.REMOTE_SERVICE_PATH).getModel();

            // For each connector
            for (ModelNode connector : EJB3RemoteResourceDefinition.CONNECTORS.resolveModelAttribute(context, remoteModel).asList()) {
                String connectorName = connector.asString();

                Map.Entry<Injector<ProtocolSocketBinding>, Injector<Registry>> entry = associationService.addConnectorInjectors(connectorName);
                associationServiceBuilder.addDependency(context.getCapabilityServiceName(CONNECTOR_CAPABILITY_NAME, connectorName, ProtocolSocketBinding.class), ProtocolSocketBinding.class, entry.getKey());
                associationServiceBuilder.addDependency(ServiceNameFactory.resolveServiceName(EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_REGISTRY, connectorName), Registry.class, entry.getValue());
            }
        }
        associationServiceBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND);
        associationServiceBuilder.install();

        //setup IIOP related stuff
        //This goes here rather than in EJB3IIOPAdd as it affects the server when it is acting as an iiop client
        //setup our dynamic stub factory
        DelegatingStubFactoryFactory.setOverriddenDynamicFactory(new DynamicStubFactoryFactory());

        //setup the substitution service, that translates between ejb proxies and IIOP stubs
        final RemoteObjectSubstitutionService substitutionService = new RemoteObjectSubstitutionService();
        context.getServiceTarget().addService(RemoteObjectSubstitutionService.SERVICE_NAME, substitutionService)
                .addDependency(DeploymentRepositoryService.SERVICE_NAME, DeploymentRepository.class, substitutionService.getDeploymentRepositoryInjectedValue())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
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
        this.defaultSecurityDomainName.set(defaultSecurityDomain);

        // set the default security domain name in the deployment unit processor, configured at the subsytem level
        final ModelNode defaultMissingMethod = EJB3SubsystemRootResourceDefinition.DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS.resolveModelAttribute(context, model);
        final boolean defaultMissingMethodValue = defaultMissingMethod.asBoolean();
        this.denyAccessByDefault.set(defaultMissingMethodValue);

        final ModelNode defaultStatefulSessionTimeout = EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT.resolveModelAttribute(context, model);
        final AtomicLong defaultTimeout = defaultStatefulSessionTimeout.isDefined() ? new AtomicLong(defaultStatefulSessionTimeout.asLong()) : DefaultStatefulBeanSessionTimeoutWriteHandler.INITIAL_TIMEOUT_VALUE;
        final ValueService defaultStatefulSessionTimeoutService = new ValueService(defaultTimeout);
        context.getServiceTarget().addService(DefaultStatefulBeanSessionTimeoutWriteHandler.SERVICE_NAME, defaultStatefulSessionTimeoutService).install();

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
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_DEFAULT_SECURITY_DOMAIN, new EJBDefaultSecurityDomainProcessor(EJB3SubsystemAdd.this.defaultSecurityDomainName, EJB3SubsystemAdd.this.knownApplicationSecurityDomains, EJB3SubsystemAdd.this.outflowSecurityDomains));
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
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_SECURITY_MISSING_METHOD_PERMISSIONS, new MissingMethodPermissionsDenyAccessMergingProcessor(EJB3SubsystemAdd.this.denyAccessByDefault));
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
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_HIBERNATE_VALIDATOR, new HibernateValidatorDeploymentUnitProcessor());

                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DEPENDS_ON_ANNOTATION, new EjbDependsOnMergingProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DEPLOYMENT_REPOSITORY, new DeploymentRepositoryProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_EJB_MANAGEMENT_RESOURCES, new EjbManagementDeploymentUnitProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_CACHE_DEPENDENCIES, new CacheDependenciesProcessor());
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_EE_MODULE_CONFIG + 1, new MdbDeliveryDependenciesProcessor()); // TODO Phase: replace by  Phase.INSTALL_MDB_DELIVERY_DEPENDENCIES

                    if (model.hasDefined(SERVER_INTERCEPTORS)) {
                        final List<ServerInterceptorMetaData> serverInterceptors = new ArrayList<>();

                        final ModelNode serverInterceptorsNode = model.get(SERVER_INTERCEPTORS);
                        for (final ModelNode serverInterceptor : serverInterceptorsNode.asList()) {
                            serverInterceptors.add(new ServerInterceptorMetaData(serverInterceptor.get("module").asString(), serverInterceptor.get("class").asString()));
                        }
                        processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_EJB_SERVER_INTERCEPTORS,
                                new StaticInterceptorsDependenciesDeploymentUnitProcessor(serverInterceptors));
                        final ServerInterceptorCache serverInterceptorCache = new ServerInterceptorCache(serverInterceptors);
                        processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_SERVER_INTERCEPTORS,
                                new ServerInterceptorsBindingsProcessor(serverInterceptorCache));
                    }

                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_GLOBAL_CLIENT_INTERCEPTORS,
                            new IdentityInterceptorProcessor());

                    if (model.hasDefined(CLIENT_INTERCEPTORS)) {
                        final List<ServerInterceptorMetaData> clientInterceptors = new ArrayList<>();

                        final ModelNode clientInterceptorsNode = model.get(CLIENT_INTERCEPTORS);
                        for (final ModelNode clientInterceptor : clientInterceptorsNode.asList()) {
                            clientInterceptors.add(new ServerInterceptorMetaData(clientInterceptor.get("module").asString(), clientInterceptor.get("class").asString()));
                        }

                        processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_EJB_SERVER_INTERCEPTORS,
                                new StaticInterceptorsDependenciesDeploymentUnitProcessor(clientInterceptors));

                        final ClientInterceptorCache clientInterceptorCache = new ClientInterceptorCache(clientInterceptors);
                        processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_SERVER_INTERCEPTORS,
                                new ClientInterceptorsBindingsProcessor(clientInterceptorCache));
                    }
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

        EJB3SubsystemDefaultCacheWriteHandler.SFSB_CACHE.updateCacheService(context, EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE.resolveModelAttribute(context, model).asStringOrNull());

        EJB3SubsystemDefaultCacheWriteHandler.SFSB_PASSIVATION_DISABLED_CACHE.updateCacheService(context, EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE.resolveModelAttribute(context, model).asStringOrNull());

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

        // this was ACTIVE
        context.getServiceTarget().addService(DeploymentRepositoryService.SERVICE_NAME, new DeploymentRepositoryService())
                .setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        // add services for making invocations from deployments
        addDeploymentInvocationServices(context, model, appclient);

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
                .addDependency(suspendControllerServiceName, SuspendController.class, ejbSuspendHandlerService.getSuspendControllerInjectedValue())
                .addDependency(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT, LocalTransactionContext.class, ejbSuspendHandlerService.getLocalTransactionContextInjectedValue())
                .addDependency(DeploymentRepositoryService.SERVICE_NAME, DeploymentRepository.class, ejbSuspendHandlerService.getDeploymentRepositoryInjectedValue())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();

        if (!appclient) {
            // create the POA Registry use by iiop
            final POARegistry poaRegistry = new POARegistry();
            context.getServiceTarget().addService(POARegistry.SERVICE_NAME, poaRegistry)
                    .addDependency(CorbaPOAService.ROOT_SERVICE_NAME, POA.class, poaRegistry.getRootPOA())
                    .setInitialMode(ServiceController.Mode.PASSIVE)
                    .install();

            StatisticsEnabledWriteHandler.INSTANCE.updateToRuntime(context, model);
        }

        TxnNamingContextFactory.setAccessChecker(new TxnNamingContextFactory.AccessChecker() {
            @Override
            public void checkAccessAllowed() throws NamingException {
                try {
                    AllowedMethodsInformation.checkAllowed(MethodType.GET_USER_TRANSACTION);
                } catch (IllegalStateException e) {
                    throw new NamingException(e.getMessage());
                }
            }
        });
    }

    /**
     * Adds remote and local invocation services for EJB clients within deployments.
     *
     * These services are required any time we need to initiate invocations on EJBs
     * from within a deployment.
     *
     * @param context the OperationContext
     * @param ejbSubsystemModel the EJB subsystem model used to configure the services
     * @param appclient true if this server is being started in reduced profile APPCLIENT mode
     * @throws OperationFailedException
     */
    private static void addDeploymentInvocationServices(final OperationContext context,
                                                        final ModelNode ejbSubsystemModel, final boolean appclient) throws OperationFailedException {

        final ServiceTarget serviceTarget = context.getServiceTarget();

        // allow the EJBClientContext to find EJBTransportProviders available and Endpoint
        final EJBClientConfiguratorService clientConfiguratorService = new EJBClientConfiguratorService();
        final ServiceBuilder<EJBClientConfiguratorService> configuratorBuilder = serviceTarget.addService(EJBClientConfiguratorService.SERVICE_NAME, clientConfiguratorService);
        if(context.hasOptionalCapability(REMOTING_ENDPOINT_CAPABILITY, EJB3SubsystemRootResourceDefinition.EJB_CLIENT_CONFIGURATOR_CAPABILITY.getName(), null)) {
            ServiceName serviceName = context.getCapabilityServiceName(REMOTING_ENDPOINT_CAPABILITY, Endpoint.class);
            configuratorBuilder.addDependency(serviceName, Endpoint.class, clientConfiguratorService.getEndpointInjector());
        }
        configuratorBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        //TODO: This should be managed
        final EJBClientContextService clientContextService = new EJBClientContextService(false);
 //       final EJBClientContextService clientContextService = new EJBClientContextService(true);
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
                    .addDependency(DeploymentRepositoryService.SERVICE_NAME, DeploymentRepository.class, byValueLocalEjbReceiver.getDeploymentRepository())
                    .setInitialMode(ServiceController.Mode.ON_DEMAND);
            byValueServiceBuilder.install();

            //the receiver for invocations that allow pass by reference
            final LocalTransportProvider byReferenceLocalEjbReceiver = new LocalTransportProvider(true);
            ServiceBuilder<LocalTransportProvider> byReferenceServiceBuilder = serviceTarget.addService(LocalTransportProvider.BY_REFERENCE_SERVICE_NAME, byReferenceLocalEjbReceiver)
                    .addDependency(DeploymentRepositoryService.SERVICE_NAME, DeploymentRepository.class, byReferenceLocalEjbReceiver.getDeploymentRepository())
                    .setInitialMode(ServiceController.Mode.ON_DEMAND);
            byReferenceServiceBuilder.install();

            // setup the default local ejb receiver service
            EJBRemoteInvocationPassByValueWriteHandler.INSTANCE.updateDefaultLocalEJBReceiverService(context, ejbSubsystemModel);
            // add the default local ejb receiver to the client context
            clientContextServiceBuilder.addDependency(LocalTransportProvider.DEFAULT_LOCAL_TRANSPORT_PROVIDER_SERVICE_NAME, EJBTransportProvider.class, clientContextService.getLocalProviderInjector());
        }
        // install the default EJB client context service
        clientContextServiceBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();
    }

    private static void addClusteringServices(final OperationContext context, final boolean appclient) {
        if (appclient) {
            return;
        }
        if (context.hasOptionalCapability(ServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR, CLUSTERED_SINGLETON_CAPABILITY, null)) {
            ServiceDependency<ServiceTargetFactory> targetFactory = ServiceDependency.on(ServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR);
            ServiceInstaller installer = new ServiceInstaller() {
                @Override
                public ServiceController<?> install(RequirementServiceTarget target) {
                    // Install on-demand singleton service
                    // We don't want this to start until a deployment requires it
                    ServiceController<?> controller = org.wildfly.service.ServiceInstaller.builder(Functions.constantSupplier(Boolean.TRUE))
                            .provides(CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName())
                            .build()
                            .install(targetFactory.get().createSingletonServiceTarget(target));

                    // Install well-known on-demand service that, once started, will force singleton service instrumentation to start.
                    ServiceInstaller.builder(Functions.constantSupplier(Boolean.TRUE))
                            .provides(ServiceNameFactory.resolveServiceName(CLUSTERED_SINGLETON_BARRIER))
                            // N.B. Depend on ServiceName(s) provided by singleton service instrumentation
                            .requires(controller.provides().stream().map(ServiceDependency::on).collect(Collectors.toList()))
                            .build()
                            .install(target);

                    return controller;
                }
            };
            ServiceInstaller.builder(installer, context.getCapabilityServiceSupport()).requires(targetFactory).build().install(context);
        }
    }

    private static final class ValueService implements Service<AtomicLong> {
        private final AtomicLong value;
        public ValueService(final AtomicLong value) {
            this.value = value;
        }

        public void start(final StartContext context) {
            // noop
        }

        public void stop(final StopContext context) {
            // noop
        }

        public AtomicLong getValue() throws IllegalStateException {
            return value;
        }
    }
}
