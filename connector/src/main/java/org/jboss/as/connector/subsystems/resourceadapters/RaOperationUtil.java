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
package org.jboss.as.connector.subsystems.resourceadapters;

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
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.VALIDATE_ON_MATCH;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATION_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAP_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTABLE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENLISTMENT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_PROPERTIES;
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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_PRINCIPAL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DOMAIN;
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
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.metadata.xmldescriptors.IronJacamarXmlDescriptor;
import org.jboss.as.connector.services.resourceadapters.deployment.InactiveResourceAdapterDeploymentService;
import org.jboss.as.connector.services.resourceadapters.deployment.ResourceAdapterXmlDeploymentService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.CopyOnWriteArrayListMultiMap;
import org.jboss.as.connector.util.ModelNodeUtil;
import org.jboss.as.connector.util.RaServicesFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.annotation.ResourceRootIndexer;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.Index;
import org.jboss.jca.common.api.metadata.common.Capacity;
import org.jboss.jca.common.api.metadata.common.Credential;
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
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.jca.common.metadata.common.PoolImpl;
import org.jboss.jca.common.metadata.common.SecurityImpl;
import org.jboss.jca.common.metadata.common.TimeOutImpl;
import org.jboss.jca.common.metadata.common.ValidationImpl;
import org.jboss.jca.common.metadata.common.XaPoolImpl;
import org.jboss.jca.common.metadata.resourceadapter.WorkManagerImpl;
import org.jboss.jca.common.metadata.resourceadapter.WorkManagerSecurityImpl;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;


public class RaOperationUtil {
    public static final ServiceName RAR_MODULE = ServiceName.of("rarinsidemodule");


    public static ModifiableResourceAdapter buildResourceAdaptersObject(final String id, final OperationContext context, ModelNode operation, String archiveOrModule) throws OperationFailedException {
        Map<String, String> configProperties = new HashMap<>(0);
        List<ConnectionDefinition> connectionDefinitions = new ArrayList<>(0);
        List<AdminObject> adminObjects = new ArrayList<>(0);
        TransactionSupportEnum transactionSupport = operation.hasDefined(TRANSACTION_SUPPORT.getName()) ? TransactionSupportEnum
                .valueOf(operation.get(TRANSACTION_SUPPORT.getName()).asString()) : null;
        String bootstrapContext = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, BOOTSTRAP_CONTEXT);
        List<String> beanValidationGroups = BEANVALIDATION_GROUPS.unwrap(context, operation);
        boolean wmSecurity = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, operation, WM_SECURITY);
        WorkManager workManager = null;

        if (wmSecurity) {
            final boolean mappingRequired = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, operation, WM_SECURITY_MAPPING_REQUIRED);
            final String domain = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, WM_SECURITY_DOMAIN);
            final String defaultPrincipal = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, WM_SECURITY_DEFAULT_PRINCIPAL);
            final List<String> defaultGroups = WM_SECURITY_DEFAULT_GROUPS.unwrap(context, operation);
            final Map<String, String> groups = ModelNodeUtil.extractMap(operation, WM_SECURITY_MAPPING_GROUPS, WM_SECURITY_MAPPING_FROM, WM_SECURITY_MAPPING_TO);
            final Map<String, String> users = ModelNodeUtil.extractMap(operation, WM_SECURITY_MAPPING_USERS, WM_SECURITY_MAPPING_FROM, WM_SECURITY_MAPPING_TO);
            workManager = new WorkManagerImpl(new WorkManagerSecurityImpl(mappingRequired, domain, defaultPrincipal, defaultGroups, users, groups));
        }

        ModifiableResourceAdapter ra;
        ra = new ModifiableResourceAdapter(id, archiveOrModule, transactionSupport, connectionDefinitions,
                adminObjects, configProperties, beanValidationGroups, bootstrapContext, workManager);

        return ra;

    }



    public static ModifiableConnDef buildConnectionDefinitionObject(final OperationContext context, final ModelNode connDefModel, final String poolName,
                                                                    final boolean isXa) throws OperationFailedException, ValidateException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        String className = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, CLASS_NAME);
        String jndiName = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, JNDINAME);
        boolean enabled = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, ENABLED);
        boolean connectable = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, CONNECTABLE);
        Boolean tracking = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, TRACKING);
        boolean useJavaContext = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, USE_JAVA_CONTEXT);
        boolean useCcm = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, USE_CCM);
        boolean sharable = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, SHARABLE);
        boolean enlistment = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, ENLISTMENT);

        int maxPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(context, connDefModel, MAX_POOL_SIZE);
        int minPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(context, connDefModel, MIN_POOL_SIZE);
        Integer initialPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(context, connDefModel, INITIAL_POOL_SIZE);
        boolean prefill = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, POOL_PREFILL);
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
            pool = new XaPoolImpl(minPoolSize, initialPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy, capacity, isSameRM, interlivng, padXid, wrapXaResource, noTxSeparatePool);
        } else {
            pool = new PoolImpl(minPoolSize, initialPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy, capacity);
        }
        String securityDomain = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, SECURITY_DOMAIN);
        String securityDomainAndApplication = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, SECURITY_DOMAIN_AND_APPLICATION);

        boolean application = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, APPLICATION);
        Security security = null;
        if (securityDomain != null || securityDomainAndApplication != null || application) {
            security = new SecurityImpl(securityDomain, securityDomainAndApplication, application);
        }
        Long backgroundValidationMillis = ModelNodeUtil.getLongIfSetOrGetDefault(context, connDefModel, BACKGROUNDVALIDATIONMILLIS);
        Boolean backgroundValidation = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, BACKGROUNDVALIDATION);
        boolean useFastFail = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, USE_FAST_FAIL);
        final Boolean validateOnMatch = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, VALIDATE_ON_MATCH );
        Validation validation = new ValidationImpl(validateOnMatch, backgroundValidation, backgroundValidationMillis, useFastFail);

        final String recoveryUsername = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, RECOVERY_USERNAME);
        final String recoveryPassword =  ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, RECOVERY_PASSWORD);
        final String recoverySecurityDomain = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, connDefModel, RECOVERY_SECURITY_DOMAIN);
        Boolean noRecovery = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, connDefModel, NO_RECOVERY);

        Recovery recovery = null;
        if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null || noRecovery != null) {
            Credential credential = null;

            if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null)
               credential = new CredentialImpl(recoveryUsername, recoveryPassword, recoverySecurityDomain);

            Extension recoverPlugin = ModelNodeUtil.extractExtension(context, connDefModel, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);

            if (noRecovery == null)
                noRecovery = Boolean.FALSE;

            recovery = new Recovery(credential, recoverPlugin, noRecovery);
        }
        ModifiableConnDef connectionDefinition = new ModifiableConnDef(configProperties, className, jndiName, poolName,
                enabled, useJavaContext, useCcm, pool, timeOut, validation, security, recovery, sharable, enlistment, connectable, tracking);

        return connectionDefinition;

    }

    public static ModifiableAdminObject buildAdminObjects(final OperationContext context, ModelNode operation, final String poolName) throws OperationFailedException, ValidateException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        String className = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, CLASS_NAME);
        String jndiName = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(context, operation, JNDINAME);
        boolean enabled = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, operation, ENABLED);
        boolean useJavaContext = ModelNodeUtil.getBooleanIfSetOrGetDefault(context, operation, USE_JAVA_CONTEXT);

        ModifiableAdminObject adminObject = new ModifiableAdminObject(configProperties, className, jndiName, poolName,
                enabled, useJavaContext);
        return adminObject;
    }


    public static ServiceName restartIfPresent(OperationContext context, final String raName, final String id, ServiceVerificationHandler svh) throws OperationFailedException {
        final ServiceName raDeploymentServiceName = ConnectorServices.getDeploymentServiceName(raName, id);

            final ServiceRegistry registry = context.getServiceRegistry(true);
            ServiceController raServiceController = registry.getService(raDeploymentServiceName);

            if (raServiceController != null) {
                final org.jboss.msc.service.ServiceController.Mode originalMode = raServiceController.getMode();
                if (svh != null) {
                    raServiceController.addListener(svh);
                }
                raServiceController.addListener(new AbstractServiceListener() {
                    @Override
                    public void transition(ServiceController controller, ServiceController.Transition transition) {
                        switch (transition) {
                            case STOPPING_to_DOWN:
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

                    @Override
                    public void listenerAdded(ServiceController controller) {
                        controller.setMode(ServiceController.Mode.NEVER);
                    }

                });
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

    public static void activate(OperationContext context, String raName, final ServiceVerificationHandler serviceVerificationHandler) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceController<?> inactiveRaController = registry.getService(ConnectorServices.INACTIVE_RESOURCE_ADAPTER_SERVICE.append(raName));

        if (inactiveRaController == null) {
            throw ConnectorLogger.ROOT_LOGGER.RARNotYetDeployed(raName);
        }
        InactiveResourceAdapterDeploymentService.InactiveResourceAdapterDeployment inactive = (InactiveResourceAdapterDeploymentService.InactiveResourceAdapterDeployment) inactiveRaController.getValue();
        final ServiceController<?> RaxmlController = registry.getService(ServiceName.of(ConnectorServices.RA_SERVICE, raName));
        Activation raxml = (Activation) RaxmlController.getValue();
        RaServicesFactory.createDeploymentService(inactive.getRegistration(), inactive.getConnectorXmlDescriptor(), inactive.getModule(), inactive.getServiceTarget(), raName, inactive.getDeploymentUnitServiceName(), inactive.getDeployment(), raxml, inactive.getResource(), serviceVerificationHandler);
    }

    public static ServiceName installRaServices(OperationContext context, ServiceVerificationHandler verificationHandler, String name, ModifiableResourceAdapter resourceAdapter, final List<ServiceController<?>> newControllers) {
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ServiceController<?> resourceAdaptersService = context.getServiceRegistry(false).getService(
                ConnectorServices.RESOURCEADAPTERS_SERVICE);

        if (resourceAdaptersService == null) {
            newControllers.add(serviceTarget.addService(ConnectorServices.RESOURCEADAPTERS_SERVICE,
                    new ResourceAdaptersService()).setInitialMode(ServiceController.Mode.ACTIVE).addListener(verificationHandler).install());
        }

        ServiceName raServiceName = ServiceName.of(ConnectorServices.RA_SERVICE, name);
        final ServiceController<?> service = context.getServiceRegistry(true).getService(raServiceName);
        if (service == null) {
            ResourceAdapterService raService = new ResourceAdapterService(resourceAdapter, name);
            newControllers.add(serviceTarget.addService(raServiceName, raService).setInitialMode(ServiceController.Mode.ACTIVE)
                    .addDependency(ConnectorServices.RESOURCEADAPTERS_SERVICE, ResourceAdaptersService.ModifiableResourceAdaptors.class, raService.getResourceAdaptersInjector())
                    .addDependency(ConnectorServices.RESOURCEADAPTERS_SUBSYSTEM_SERVICE, CopyOnWriteArrayListMultiMap.class, raService.getResourceAdaptersMapInjector())
                    .addListener(verificationHandler).install());
        }
        return raServiceName;
    }

    public static void installRaServicesAndDeployFromModule(OperationContext context, ServiceVerificationHandler verificationHandler, String name, ModifiableResourceAdapter resourceAdapter, String fullModuleName, final List<ServiceController<?>> newControllers) throws OperationFailedException{
        ServiceName raServiceName =  installRaServices(context, verificationHandler, name, resourceAdapter, newControllers);
        final boolean resolveProperties = true;
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final String moduleName;


        //load module
        String slot = "main";
        if (fullModuleName.contains(":")) {
            slot = fullModuleName.substring(fullModuleName.indexOf(":") + 1);
            moduleName = fullModuleName.substring(0, fullModuleName.indexOf(":"));
        } else {
            moduleName = fullModuleName;
        }

        Module module;
        try {
            ModuleIdentifier moduleId = ModuleIdentifier.create(moduleName, slot);
            module = Module.getCallerModuleLoader().loadModule(moduleId);
        } catch (ModuleLoadException e) {
            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToLoadModuleRA(moduleName), e);
        }
        URL path = module.getExportedResource("META-INF/ra.xml");
        Closeable closable = null;
            try {
                VirtualFile child;
                if (path.getPath().contains("!")) {
                    throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.compressedRarNotSupportedInModuleRA(moduleName));
                } else {
                    child = VFS.getChild(path.getPath().split("META-INF")[0]);

                    closable = VFS.mountReal(new File(path.getPath().split("META-INF")[0]), child);
                }
                //final Closeable closable = VFS.mountZip((InputStream) new JarInputStream(new FileInputStream(path.getPath().split("!")[0].split(":")[1])), path.getPath().split("!")[0].split(":")[1], child, TempFileProviderService.provider());

                final MountHandle mountHandle = new MountHandle(closable);
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
                if (ironJacamarXmlDescriptor != null) {
                    ConnectorLogger.SUBSYSTEM_RA_LOGGER.forceIJToNull();
                    ironJacamarXmlDescriptor = null;
                }
                final ServiceName deployerServiceName = ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(connectorXmlDescriptor.getDeploymentName());
                final ServiceController<?> deployerService = context.getServiceRegistry(true).getService(deployerServiceName);
                if (deployerService == null) {
                    ServiceBuilder builder = ParsedRaDeploymentProcessor.process(connectorXmlDescriptor, ironJacamarXmlDescriptor, module.getClassLoader(), serviceTarget, annotationIndexes, RAR_MODULE.append(name), verificationHandler);
                    newControllers.add(builder.addDependency(raServiceName).setInitialMode(ServiceController.Mode.ACTIVE).install());
                }
                String rarName = resourceAdapter.getArchive();

                if (fullModuleName.equals(rarName)) {

                    ServiceName serviceName = ConnectorServices.INACTIVE_RESOURCE_ADAPTER_SERVICE.append(name);

                    InactiveResourceAdapterDeploymentService service = new InactiveResourceAdapterDeploymentService(connectorXmlDescriptor, module, name, name, RAR_MODULE.append(name), null, serviceTarget, null);
                    newControllers.add(serviceTarget
                            .addService(serviceName, service)
                            .setInitialMode(ServiceController.Mode.ACTIVE).addListener(verificationHandler).install());

                }

            } catch (Exception e) {
                throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToLoadModuleRA(moduleName), e);
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
