/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector._private.Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY;
import static org.jboss.as.connector._private.Capabilities.ELYTRON_SECURITY_DOMAIN_CAPABILITY;
import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;
import static org.jboss.as.connector.subsystems.common.jndi.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.common.jndi.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.VALIDATE_ON_MATCH;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.AUTHENTICATION_CONTEXT_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATION_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAP_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTABLE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENLISTMENT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENLISTMENT_TRACE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MCP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVER_PLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVER_PLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SHARABLE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRACKING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTION_SUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_ELYTRON_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_PRINCIPAL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_FROM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_REQUIRED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_TO;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_USERS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.connector.deployers.ra.processors.IronJacamarDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.ra.processors.ParsedRaDeploymentProcessor;
import org.jboss.as.connector.deployers.ra.processors.RaDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.ra.processors.RaNativeProcessor;
import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.common.CredentialImpl;
import org.jboss.as.connector.metadata.common.SecurityImpl;
import org.jboss.as.connector.metadata.resourceadapter.WorkManagerSecurityImpl;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.metadata.xmldescriptors.IronJacamarXmlDescriptor;
import org.jboss.as.connector.services.resourceadapters.deployment.InactiveResourceAdapterDeploymentService;
import org.jboss.as.connector.services.resourceadapters.deployment.ResourceAdapterXmlDeploymentService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.CopyOnWriteArrayListMultiMap;
import org.jboss.as.connector.util.ModelNodeUtil;
import org.jboss.as.connector.util.RaServicesFactory;
import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.UninterruptibleCountDownLatch;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.annotation.ResourceRootIndexer;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.Index;
import org.jboss.jca.common.api.metadata.common.Capacity;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.common.Pool;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.Security;
import org.jboss.jca.common.api.metadata.common.TimeOut;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.common.Validation;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.AdminObject;
import org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.resourceadapter.WorkManager;
import org.jboss.jca.common.api.metadata.resourceadapter.WorkManagerSecurity;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.PoolImpl;
import org.jboss.jca.common.metadata.common.TimeOutImpl;
import org.jboss.jca.common.metadata.common.ValidationImpl;
import org.jboss.jca.common.metadata.common.XaPoolImpl;
import org.jboss.jca.common.metadata.resourceadapter.WorkManagerImpl;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.credential.source.CredentialSource;


public class RaOperationUtil {

    private static final ServiceName SECURITY_DOMAIN_SERVICE = ServiceName.JBOSS.append("security", "security-domain");

    public static final ServiceName RAR_MODULE = ServiceName.of("rarinsidemodule");


    public static ModifiableResourceAdapter buildResourceAdaptersObject(final String id, final OperationContext context, ModelNode operation, String archiveOrModule) throws OperationFailedException {
        Map<String, String> configProperties = new HashMap<>(0);
        List<ConnectionDefinition> connectionDefinitions = new ArrayList<>(0);
        List<AdminObject> adminObjects = new ArrayList<>(0);
        String transactionSupportResolved = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, TRANSACTION_SUPPORT);
        TransactionSupportEnum transactionSupport = operation.hasDefined(TRANSACTION_SUPPORT.getName()) ? TransactionSupportEnum
                .valueOf(ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, TRANSACTION_SUPPORT)) : null;
        String bootstrapContext = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, BOOTSTRAP_CONTEXT);
        List<String> beanValidationGroups = BEANVALIDATION_GROUPS.unwrap(context, operation);
        boolean wmSecurity = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, operation, WM_SECURITY);
        WorkManager workManager = null;

        if (wmSecurity) {
            final boolean mappingRequired = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, operation, WM_SECURITY_MAPPING_REQUIRED);
            String domain;
            final String elytronDomain = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, WM_ELYTRON_SECURITY_DOMAIN);
            if (elytronDomain != null) {
                domain = elytronDomain;
            } else {
                throw new OperationFailedException(SUBSYSTEM_RA_LOGGER.legacySecurityNotSupported());
            }
            final String defaultPrincipal = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, WM_SECURITY_DEFAULT_PRINCIPAL);
            final List<String> defaultGroups = WM_SECURITY_DEFAULT_GROUPS.unwrap(context, operation);
            final Map<String, String> groups = ModelNodeUtil.extractMap(operation, WM_SECURITY_MAPPING_GROUPS, WM_SECURITY_MAPPING_FROM, WM_SECURITY_MAPPING_TO);
            final Map<String, String> users = ModelNodeUtil.extractMap(operation, WM_SECURITY_MAPPING_USERS, WM_SECURITY_MAPPING_FROM, WM_SECURITY_MAPPING_TO);
            workManager = new WorkManagerImpl(new WorkManagerSecurityImpl(mappingRequired, domain, elytronDomain != null, defaultPrincipal, defaultGroups, users, groups));
        }

        ModifiableResourceAdapter ra;
        ra = new ModifiableResourceAdapter(id, archiveOrModule, transactionSupport, connectionDefinitions,
                adminObjects, configProperties, beanValidationGroups, bootstrapContext, workManager);

        return ra;

    }


    public static ModifiableConnDef buildConnectionDefinitionObject(final OperationContext context, final ModelNode connDefModel, final String poolName,
                                                                    final boolean isXa, ExceptionSupplier<CredentialSource, Exception> recoveryCredentialSourceSupplier) throws OperationFailedException, ValidateException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        String className = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, CLASS_NAME);
        String jndiName = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, JNDI_NAME);
        boolean enabled = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, ENABLED);
        boolean connectable = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, CONNECTABLE);
        Boolean tracking = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, TRACKING);
        boolean useJavaContext = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, USE_JAVA_CONTEXT);
        boolean useCcm = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, USE_CCM);
        boolean sharable = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, SHARABLE);
        boolean enlistment = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, ENLISTMENT);

        final String mcp = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, MCP);
        final Boolean enlistmentTrace = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, ENLISTMENT_TRACE);


        int maxPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(context, connDefModel, MAX_POOL_SIZE);
        int minPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(context, connDefModel, MIN_POOL_SIZE);
        Integer initialPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(context, connDefModel, INITIAL_POOL_SIZE);
        boolean prefill = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, POOL_PREFILL);
        boolean fair = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, POOL_FAIR);
        boolean useStrictMin = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, POOL_USE_STRICT_MIN);
        String flushStrategyString = POOL_FLUSH_STRATEGY.resolveModelAttribute(context, connDefModel).asString();
        final FlushStrategy flushStrategy = FlushStrategy.forName(flushStrategyString);
        Boolean isSameRM  = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, SAME_RM_OVERRIDE);
        boolean interlivng = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, INTERLEAVING);
        boolean padXid = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, PAD_XID);
        boolean wrapXaResource = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, WRAP_XA_RESOURCE);
        boolean noTxSeparatePool = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, NOTXSEPARATEPOOL);



        Integer allocationRetry = ModelNodeUtil.getIntIfSetOrGetDefault(context, connDefModel, ALLOCATION_RETRY);
        Long allocationRetryWaitMillis = ModelNodeUtil.getLongIfSetOrGetDefault(context, connDefModel, ALLOCATION_RETRY_WAIT_MILLIS);
        Long blockingTimeoutMillis = ModelNodeUtil.getLongIfSetOrGetDefault(context, connDefModel, BLOCKING_TIMEOUT_WAIT_MILLIS);
        Long idleTimeoutMinutes = ModelNodeUtil.getLongIfSetOrGetDefault(context, connDefModel, IDLETIMEOUTMINUTES);
        Integer xaResourceTimeout = ModelNodeUtil.getIntIfSetOrGetDefault(context, connDefModel, XA_RESOURCE_TIMEOUT);

        TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout);

        Extension incrementer = ModelNodeUtil.extractExtension(context, connDefModel, CAPACITY_INCREMENTER_CLASS, CAPACITY_INCREMENTER_PROPERTIES);
        Extension decrementer = ModelNodeUtil.extractExtension(context, connDefModel, CAPACITY_DECREMENTER_CLASS, CAPACITY_DECREMENTER_PROPERTIES);
        final Capacity capacity = new Capacity(incrementer, decrementer);

        Pool pool;
        if (isXa) {
            pool = new XaPoolImpl(minPoolSize, initialPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy, capacity, fair, isSameRM, interlivng, padXid, wrapXaResource, noTxSeparatePool);
        } else {
            pool = new PoolImpl(minPoolSize, initialPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy, capacity, fair);
        }
        String securityDomain = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, SECURITY_DOMAIN);
        String securityDomainAndApplication = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, SECURITY_DOMAIN_AND_APPLICATION);
        if (securityDomain != null || securityDomainAndApplication != null) {
            throw new OperationFailedException(SUBSYSTEM_RA_LOGGER.legacySecurityNotSupported());
        }
        String authenticationContext = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, AUTHENTICATION_CONTEXT);
        String authenticationContextAndApplication = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, AUTHENTICATION_CONTEXT_AND_APPLICATION);


        boolean application = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, APPLICATION);
        Security security = null;
        if (authenticationContext != null || authenticationContextAndApplication != null || application) {
            security = new SecurityImpl(authenticationContext,
                    authenticationContextAndApplication, application);
        }
        Long backgroundValidationMillis = ModelNodeUtil.getLongIfSetOrGetDefault(context, connDefModel, BACKGROUNDVALIDATIONMILLIS);
        Boolean backgroundValidation = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, BACKGROUNDVALIDATION);
        boolean useFastFail = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, USE_FAST_FAIL);
        final Boolean validateOnMatch = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, VALIDATE_ON_MATCH );
        Validation validation = new ValidationImpl(validateOnMatch, backgroundValidation, backgroundValidationMillis, useFastFail);

        final String recoveryUsername = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, RECOVERY_USERNAME);
        final String recoveryPassword =  ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, RECOVERY_PASSWORD);

        final String recoverySecurityDomain = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, RECOVERY_SECURITY_DOMAIN);
        if (recoverySecurityDomain != null) {
            throw new OperationFailedException(SUBSYSTEM_RA_LOGGER.legacySecurityNotSupported());
        }
        final String recoveryAuthenticationContext = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, RECOVERY_AUTHENTICATION_CONTEXT);
        Boolean noRecovery = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, NO_RECOVERY);


        Recovery recovery = null;
        if ((recoveryUsername != null && (recoveryPassword != null || recoveryCredentialSourceSupplier != null)) || recoveryAuthenticationContext != null || noRecovery != null) {
            Credential credential = null;

            if ((recoveryUsername != null && (recoveryPassword != null || recoveryCredentialSourceSupplier != null)) || recoveryAuthenticationContext != null)
                credential = new CredentialImpl(recoveryUsername, recoveryPassword,
                        recoveryAuthenticationContext, recoveryCredentialSourceSupplier);

            Extension recoverPlugin = ModelNodeUtil.extractExtension(context, connDefModel, RECOVER_PLUGIN_CLASSNAME, RECOVER_PLUGIN_PROPERTIES);

            if (noRecovery == null)
                noRecovery = Boolean.FALSE;

            recovery = new Recovery(credential, recoverPlugin, noRecovery);
        }
        ModifiableConnDef connectionDefinition = new ModifiableConnDef(configProperties, className, jndiName, poolName,
                enabled, useJavaContext, useCcm, pool, timeOut, validation, security, recovery, sharable, enlistment, connectable, tracking, mcp, enlistmentTrace);

        return connectionDefinition;

    }

    public static ModifiableAdminObject buildAdminObjects(final OperationContext context, ModelNode operation, final String poolName) throws OperationFailedException, ValidateException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        String className = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, CLASS_NAME);
        String jndiName = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, JNDI_NAME);
        boolean enabled = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, operation, ENABLED);
        boolean useJavaContext = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, operation, USE_JAVA_CONTEXT);

        ModifiableAdminObject adminObject = new ModifiableAdminObject(configProperties, className, jndiName, poolName,
                enabled, useJavaContext);
        return adminObject;
    }


    public static ServiceName restartIfPresent(OperationContext context, final String raName, final String id) throws OperationFailedException {
        final ServiceName raDeploymentServiceName = ConnectorServices.getDeploymentServiceName(raName, id);

            final ServiceRegistry registry = context.getServiceRegistry(true);
            ServiceController raServiceController = registry.getService(raDeploymentServiceName);

            if (raServiceController != null) {
                final ServiceController.Mode originalMode = raServiceController.getMode();
                final UninterruptibleCountDownLatch latch = new UninterruptibleCountDownLatch(1);
                raServiceController.addListener(new LifecycleListener() {
                    @Override
                    public void handleEvent(ServiceController controller, LifecycleEvent event) {
                        latch.awaitUninterruptibly();
                        if (event == LifecycleEvent.DOWN) {
                            try {
                                final ServiceController<?> RaxmlController = registry.getService(ServiceName.of(ConnectorServices.RA_SERVICE, id));
                                Activation raxml = (Activation) RaxmlController.getValue();
                                ((ResourceAdapterXmlDeploymentService) controller.getService()).setRaxml(raxml);
                                controller.compareAndSetMode(ServiceController.Mode.NEVER, originalMode);
                            } finally {
                                controller.removeListener(this);
                            }
                        }
                    }
                });
                try {
                    raServiceController.setMode(ServiceController.Mode.NEVER);
                } finally {
                    latch.countDown();
                }
                return raDeploymentServiceName;
            } else {
                return null;
            }
    }

    public static boolean removeIfActive(OperationContext context, String raName, String id ) throws OperationFailedException {
        boolean wasActive = false;
        final ServiceName raDeploymentServiceName;
        if (raName == null) {
            raDeploymentServiceName = ConnectorServices.getDeploymentServiceName(id);
        } else {
            raDeploymentServiceName = ConnectorServices.getDeploymentServiceName(raName, id);
        }

        if(context.getServiceRegistry(true).getService(raDeploymentServiceName) != null){
            context.removeService(raDeploymentServiceName);
            wasActive = true;
        }

        return wasActive;

    }

    public static void activate(OperationContext context, String raName, String archiveName) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> inactiveRaController = registry.getService(ConnectorServices.INACTIVE_RESOURCE_ADAPTER_SERVICE.append(archiveName));
        final CapabilityServiceSupport support = context.getCapabilityServiceSupport();

        if (inactiveRaController == null) {

            inactiveRaController = registry.getService(ConnectorServices.INACTIVE_RESOURCE_ADAPTER_SERVICE.append(raName));

            if (inactiveRaController == null) {

                throw ConnectorLogger.ROOT_LOGGER.RARNotYetDeployed(raName);
            }
        }
        InactiveResourceAdapterDeploymentService.InactiveResourceAdapterDeployment inactive = (InactiveResourceAdapterDeploymentService.InactiveResourceAdapterDeployment) inactiveRaController.getValue();
        final ServiceController<?> RaxmlController = registry.getService(ServiceName.of(ConnectorServices.RA_SERVICE, raName));
        Activation raxml = (Activation) RaxmlController.getValue();
        RaServicesFactory.createDeploymentService(inactive.getRegistration(), inactive.getConnectorXmlDescriptor(), inactive.getModule(), inactive.getServiceTarget(),
                archiveName, inactive.getDeploymentUnitServiceName(), inactive.getDeployment(), raxml, inactive.getResource(), registry, support);
    }

    public static ServiceName installRaServices(OperationContext context, String name, ModifiableResourceAdapter resourceAdapter, final List<ServiceController<?>> newControllers) {
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ServiceController<?> resourceAdaptersService = context.getServiceRegistry(false).getService(
                ConnectorServices.RESOURCEADAPTERS_SERVICE);

        if (resourceAdaptersService == null) {
            newControllers.add(serviceTarget.addService(ConnectorServices.RESOURCEADAPTERS_SERVICE,
                    new ResourceAdaptersService()).setInitialMode(ServiceController.Mode.ACTIVE).install());
        }

        ServiceName raServiceName = ServiceName.of(ConnectorServices.RA_SERVICE, name);
        final ServiceController<?> service = context.getServiceRegistry(true).getService(raServiceName);
        if (service == null) {
            ResourceAdapterService raService = new ResourceAdapterService(resourceAdapter, name);
            ServiceBuilder builder = serviceTarget.addService(raServiceName, raService).setInitialMode(ServiceController.Mode.ACTIVE)
                    .addDependency(ConnectorServices.RESOURCEADAPTERS_SERVICE, ResourceAdaptersService.ModifiableResourceAdaptors.class, raService.getResourceAdaptersInjector())
                    .addDependency(ConnectorServices.RESOURCEADAPTERS_CONFIGURED_ADAPTERS_SERVICE, CopyOnWriteArrayListMultiMap.class, raService.getResourceAdaptersSubsystemInjector());
            // add dependency on security domain service if applicable for recovery config
            for (ConnectionDefinition cd : resourceAdapter.getConnectionDefinitions()) {
                Security security = cd.getSecurity();
                if (security != null) {
                    if (security.getSecurityDomain() != null) {
                        builder.requires(context.getCapabilityServiceName(AUTHENTICATION_CONTEXT_CAPABILITY, security.getSecurityDomain(), AuthenticationContext.class));
                    }
                    if (security.getSecurityDomainAndApplication() != null) {
                        builder.requires(context.getCapabilityServiceName(AUTHENTICATION_CONTEXT_CAPABILITY, security.getSecurityDomainAndApplication(), AuthenticationContext.class));
                    }
                    if (cd.getRecovery() != null && cd.getRecovery().getCredential() != null && cd.getRecovery().getCredential().getSecurityDomain() != null) {
                        builder.requires(context.getCapabilityServiceName(AUTHENTICATION_CONTEXT_CAPABILITY, cd.getRecovery().getCredential().getSecurityDomain(), AuthenticationContext.class));
                    }
                }
            }
            if (resourceAdapter.getWorkManager() != null) {
                final WorkManagerSecurity workManagerSecurity = resourceAdapter.getWorkManager().getSecurity();
                if (workManagerSecurity != null) {
                    final String securityDomainName = workManagerSecurity.getDomain();
                    if (securityDomainName != null) {
                        builder.requires(context.getCapabilityServiceName(ELYTRON_SECURITY_DOMAIN_CAPABILITY, securityDomainName, SecurityDomain.class));
                    }
                }
            }
            newControllers.add(builder.install());
        }
        return raServiceName;
    }

    public static void installRaServicesAndDeployFromModule(OperationContext context, String name, ModifiableResourceAdapter resourceAdapter, String fullModuleName, final List<ServiceController<?>> newControllers) throws OperationFailedException{
        ServiceName raServiceName =  installRaServices(context, name, resourceAdapter, newControllers);
        final boolean resolveProperties = true;
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final String moduleName;
        final CapabilityServiceSupport support = context.getCapabilityServiceSupport();


        //load module
        String slot = "main";
        if (fullModuleName.contains(":")) {
            slot = fullModuleName.substring(fullModuleName.indexOf(":") + 1);
            moduleName = fullModuleName.substring(0, fullModuleName.indexOf(":"));
        } else {
            moduleName = fullModuleName;
        }

        Module module;
        String moduleId = ModuleIdentifierUtil.canonicalModuleIdentifier(moduleName, slot);
        try {
            module = Module.getCallerModuleLoader().loadModule(moduleId);
        } catch (ModuleNotFoundException e) {
            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.raModuleNotFound(moduleId, e.getMessage()), e);
        } catch (ModuleLoadException e) {
            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToLoadModuleRA(moduleId, e.getMessage()), e);
        }
        URL path = module.getExportedResource("META-INF/ra.xml");
        if (path == null) {
            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.raXmlNotFound(moduleId));
        }
        Closeable closable = null;
            try {
                VirtualFile child;
                if (path.getPath().contains("!")) {
                    throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.compressedRarNotSupportedInModuleRA(moduleId));
                } else {
                    child = VFS.getChild(path.getPath().split("META-INF")[0]);

                    closable = VFS.mountReal(new File(path.getPath().split("META-INF")[0]), child);
                }
                //final Closeable closable = VFS.mountZip((InputStream) new JarInputStream(new FileInputStream(path.getPath().split("!")[0].split(":")[1])), path.getPath().split("!")[0].split(":")[1], child, TempFileProviderService.provider());

                final MountHandle mountHandle = MountHandle.create(closable);
                final ResourceRoot resourceRoot = new ResourceRoot(child, mountHandle);

                final VirtualFile deploymentRoot = resourceRoot.getRoot();
                if (deploymentRoot == null || !deploymentRoot.exists())
                    return;
                ConnectorXmlDescriptor connectorXmlDescriptor = RaDeploymentParsingProcessor.process(resolveProperties, deploymentRoot, null, name);
                IronJacamarXmlDescriptor ironJacamarXmlDescriptor = IronJacamarDeploymentParsingProcessor.process(deploymentRoot, resolveProperties);
                RaNativeProcessor.process(deploymentRoot);
                Map<ResourceRoot, Index> annotationIndexes = new HashMap<ResourceRoot, Index>();
                ResourceRootIndexer.indexResourceRoot(resourceRoot);
                Index index = resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX);
                if (index != null) {
                    annotationIndexes.put(resourceRoot, index);
                }
                final List<VirtualFile> jarChildren = deploymentRoot.getChildren(new SuffixMatchFilter(".jar", VisitorAttributes.LEAVES_ONLY));
                for (VirtualFile subJarRoot: jarChildren) {
                    Closeable subClosable = VFS.mountZip(subJarRoot.getPhysicalFile(), subJarRoot, TempFileProviderService.provider());
                    ResourceRoot subResRoot = new ResourceRoot(subJarRoot, MountHandle.create(subClosable));
                    ResourceRootIndexer.indexResourceRoot(subResRoot);
                    Index subIndex = subResRoot.getAttachment(Attachments.ANNOTATION_INDEX);
                    if (subIndex != null) {
                        annotationIndexes.put(subResRoot, subIndex);
                    }
                }
                if (ironJacamarXmlDescriptor != null) {
                    ConnectorLogger.SUBSYSTEM_RA_LOGGER.forceIJToNull();
                    ironJacamarXmlDescriptor = null;
                }
                final ServiceName deployerServiceName = ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(connectorXmlDescriptor.getDeploymentName());
                final ServiceController<?> deployerService = context.getServiceRegistry(true).getService(deployerServiceName);

                if (deployerService == null) {
                    ServiceBuilder builder = ParsedRaDeploymentProcessor.process(connectorXmlDescriptor, ironJacamarXmlDescriptor, module.getClassLoader(), serviceTarget, annotationIndexes, RAR_MODULE.append(name), null, null, support);
                    builder.requires(raServiceName);
                    newControllers.add(builder.setInitialMode(ServiceController.Mode.ACTIVE).install());
                }
                String rarName = resourceAdapter.getArchive();

                if (fullModuleName.equals(rarName)) {

                    ServiceName serviceName = ConnectorServices.INACTIVE_RESOURCE_ADAPTER_SERVICE.append(name);

                    InactiveResourceAdapterDeploymentService service = new InactiveResourceAdapterDeploymentService(connectorXmlDescriptor, module, name, name, RAR_MODULE.append(name), null, serviceTarget, null);
                    newControllers.add(serviceTarget
                            .addService(serviceName, service)
                            .setInitialMode(ServiceController.Mode.ACTIVE).install());

                }

            } catch (Exception e) {
                throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToLoadModuleRA(moduleId, e.getMessage()), e);
            } finally {
                if (closable != null) {
                    try {
                        closable.close();
                    } catch (IOException e) {

                    }
                }
            }


    }


}
