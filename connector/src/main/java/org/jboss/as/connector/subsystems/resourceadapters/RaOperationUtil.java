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
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.RaServicesFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.annotation.ResourceRootIndexer;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.Index;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.common.v10.CommonConnDef;
import org.jboss.jca.common.api.metadata.resourceadapter.v10.ResourceAdapter;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonSecurityImpl;
import org.jboss.jca.common.metadata.common.CommonTimeOutImpl;
import org.jboss.jca.common.metadata.common.CommonValidationImpl;
import org.jboss.jca.common.metadata.common.CommonXaPoolImpl;
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import static org.jboss.as.connector.logging.ConnectorMessages.MESSAGES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATION_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAP_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTION_SUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;

public class RaOperationUtil {
    private static final String RAR_EXTENSION = ".rar";
    private static final ServiceName RAR_MODULE = ServiceName.of("rarinsidemodule");


    public static ModifiableResourceAdapter buildResourceAdaptersObject(final OperationContext context, ModelNode operation, String archiveOrModule) throws OperationFailedException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        List<CommonConnDef> connectionDefinitions = new ArrayList<CommonConnDef>(0);
        List<CommonAdminObject> adminObjects = new ArrayList<CommonAdminObject>(0);
        TransactionSupportEnum transactionSupport = operation.hasDefined(TRANSACTION_SUPPORT.getName()) ? TransactionSupportEnum
                .valueOf(operation.get(TRANSACTION_SUPPORT.getName()).asString()) : null;
        String bootstrapContext = BOOTSTRAP_CONTEXT.resolveModelAttribute(context, operation).asString();
        List<String> beanValidationGroups = null;
        if (operation.hasDefined(BEANVALIDATION_GROUPS.getName())) {
            beanValidationGroups = new ArrayList<String>(operation.get(BEANVALIDATION_GROUPS.getName()).asList().size());
            for (ModelNode beanValidation : operation.get(BEANVALIDATION_GROUPS.getName()).asList()) {
                beanValidationGroups.add(beanValidation.asString());
            }

        }
        ModifiableResourceAdapter ra;
        ra = new ModifiableResourceAdapter(archiveOrModule, transactionSupport, connectionDefinitions,
                adminObjects, configProperties, beanValidationGroups, bootstrapContext);

        return ra;

    }

    public static ModifiableConnDef buildConnectionDefinitionObject(final OperationContext context, final ModelNode recoveryEnvModel, final String poolName,
                                                                    final boolean isXa) throws OperationFailedException, ValidateException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        String className = CLASS_NAME.resolveModelAttribute(context, recoveryEnvModel).asString();
        String jndiName = JNDINAME.resolveModelAttribute(context, recoveryEnvModel).asString();
        boolean enabled = ENABLED.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        boolean useJavaContext = USE_JAVA_CONTEXT.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        boolean useCcm = USE_CCM.resolveModelAttribute(context, recoveryEnvModel).asBoolean();

        int maxPoolSize = MAX_POOL_SIZE.resolveModelAttribute(context, recoveryEnvModel).asInt();
        int minPoolSize = MIN_POOL_SIZE.resolveModelAttribute(context, recoveryEnvModel).asInt();
        boolean prefill = POOL_PREFILL.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        boolean useStrictMin = POOL_USE_STRICT_MIN.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        String flushStrategyString = POOL_FLUSH_STRATEGY.resolveModelAttribute(context, recoveryEnvModel).asString();
        final FlushStrategy flushStrategy = FlushStrategy.forName(flushStrategyString);
        boolean isSameRM = SAME_RM_OVERRIDE.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        boolean interlivng = INTERLEAVING.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        boolean padXid = PAD_XID.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        boolean wrapXaResource = WRAP_XA_RESOURCE.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        boolean noTxSeparatePool = NOTXSEPARATEPOOL.resolveModelAttribute(context, recoveryEnvModel).asBoolean();

        ModelNode allocationRetryModel = ALLOCATION_RETRY.resolveModelAttribute(context, recoveryEnvModel);
        ModelNode allocationRetryWaitMillisModel = ALLOCATION_RETRY_WAIT_MILLIS.resolveModelAttribute(context, recoveryEnvModel);
        ModelNode blockingTimeoutMillisModel = BLOCKING_TIMEOUT_WAIT_MILLIS.resolveModelAttribute(context, recoveryEnvModel);
        ModelNode idleTimeoutMinutesModel = IDLETIMEOUTMINUTES.resolveModelAttribute(context, recoveryEnvModel);
        ModelNode xaResourceTimeoutModel = XA_RESOURCE_TIMEOUT.resolveModelAttribute(context, recoveryEnvModel);


        Integer allocationRetry = allocationRetryModel.isDefined()?allocationRetryModel.asInt():null;
        Long allocationRetryWaitMillis = allocationRetryWaitMillisModel.isDefined()?allocationRetryWaitMillisModel.asLong():null;
        Long blockingTimeoutMillis = blockingTimeoutMillisModel.isDefined()?blockingTimeoutMillisModel.asLong():null;
        Long idleTimeoutMinutes = idleTimeoutMinutesModel.isDefined()?idleTimeoutMinutesModel.asLong():null;
        Integer xaResourceTimeout = xaResourceTimeoutModel.isDefined()?xaResourceTimeoutModel.asInt():null;

        CommonTimeOut timeOut = new CommonTimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout);
        CommonPool pool;
        if (isXa) {
            pool = new CommonXaPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy, isSameRM, interlivng, padXid, wrapXaResource, noTxSeparatePool);
        } else {
            pool = new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy);
        }
        ModelNode securityDomainModel = SECURITY_DOMAIN.resolveModelAttribute(context, recoveryEnvModel);
        String securityDomain = securityDomainModel.isDefined()?securityDomainModel.asString():null;
        ModelNode securityDomainAndApplicationModel = SECURITY_DOMAIN_AND_APPLICATION.resolveModelAttribute(context, recoveryEnvModel);
        String securityDomainAndApplication = securityDomainAndApplicationModel.isDefined()?securityDomainAndApplicationModel.asString():null;

        boolean application = APPLICATION.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        CommonSecurity security = null;
        if (securityDomain != null || securityDomainAndApplication != null) {
            security = new CommonSecurityImpl(securityDomain, securityDomainAndApplication, application);
        }
        ModelNode backgroundValidationMillisModel = BACKGROUNDVALIDATIONMILLIS.resolveModelAttribute(context, recoveryEnvModel);
        Long backgroundValidationMillis = backgroundValidationMillisModel.isDefined()?backgroundValidationMillisModel.asLong():null;
        boolean backgroundValidation = BACKGROUNDVALIDATION.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        boolean useFastFail = USE_FAST_FAIL.resolveModelAttribute(context, recoveryEnvModel).asBoolean();
        CommonValidation validation = new CommonValidationImpl(backgroundValidation, backgroundValidationMillis, useFastFail);

        final String recoveryUsername = recoveryEnvModel.hasDefined(RECOVERY_USERNAME.getName()) ? RECOVERY_USERNAME.resolveModelAttribute(context, recoveryEnvModel).asString() : null;

        final String recoveryPassword =  recoveryEnvModel.hasDefined(RECOVERY_PASSWORD.getName()) ? RECOVERY_PASSWORD.resolveModelAttribute(context, recoveryEnvModel).asString() : null;
        final ModelNode recoverySecurityDomainModel = RECOVERY_SECURITY_DOMAIN.resolveModelAttribute(context, recoveryEnvModel);
        final String recoverySecurityDomain = recoverySecurityDomainModel.isDefined()?recoverySecurityDomainModel.asString():null;
        boolean noRecovery = NO_RECOVERY.resolveModelAttribute(context, recoveryEnvModel).asBoolean();

        Recovery recovery = null;
        if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null) {
            Credential credential = null;
            credential = new CredentialImpl(recoveryUsername, recoveryPassword, recoverySecurityDomain);
            Extension recoverPlugin = extractExtension(context, recoveryEnvModel, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);
            recovery = new Recovery(credential, recoverPlugin, noRecovery);
        }
        ModifiableConnDef connectionDefinition = new ModifiableConnDef(configProperties, className, jndiName, poolName,
                enabled, useJavaContext, useCcm, pool, timeOut, validation, security, recovery);

        return connectionDefinition;

    }

    public static ModifiableAdminObject buildAdminObjects(final OperationContext context, ModelNode operation, final String poolName) throws OperationFailedException, ValidateException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        String className = CLASS_NAME.resolveModelAttribute(context, operation).asString();
        String jndiName = JNDINAME.resolveModelAttribute(context, operation).asString();
        boolean enabled = ENABLED.resolveModelAttribute(context, operation).asBoolean();
        boolean useJavaContext = USE_JAVA_CONTEXT.resolveModelAttribute(context, operation).asBoolean();

        ModifiableAdminObject adminObject = new ModifiableAdminObject(configProperties, className, jndiName, poolName,
                enabled, useJavaContext);
        return adminObject;
    }

    private static Extension extractExtension(final OperationContext operationContext, final ModelNode node,
                                              final SimpleAttributeDefinition className, final PropertiesAttributeDefinition propertyName)
            throws ValidateException, OperationFailedException {
        if (node.hasDefined(className.getName())) {
            String exceptionSorterClassName = className.resolveModelAttribute(operationContext, node).asString();
            Map<String, String> unwrapped = propertyName.unwrap(operationContext, node);
            Map<String, String> exceptionSorterProperty = unwrapped.size() > 0 ? unwrapped : null;
            return new Extension(exceptionSorterClassName, exceptionSorterProperty);
        } else {
            return null;
        }
    }

    public static boolean deactivateIfActive(OperationContext context, String raName) throws OperationFailedException {
        boolean wasActive = false;
        final ServiceName raDeploymentServiceName = ConnectorServices.getDeploymentServiceName(raName);
        Integer identifier = 0;
        if (raName.contains("->")) {
            identifier = Integer.valueOf(raName.substring(raName.indexOf("->") + 2));
            raName = raName.substring(0, raName.indexOf("->"));
        }
        if (raDeploymentServiceName != null) {
            context.removeService(raDeploymentServiceName);
            ConnectorServices.unregisterDeployment(raName, raDeploymentServiceName);
            wasActive = true;
        }
        ConnectorServices.unregisterResourceIdentifier(raName, identifier);

        ServiceName deploymentServiceName = ConnectorServices.getDeploymentServiceName(raName);

        return wasActive;

    }

    public static void activate(OperationContext context, String raName, String rarName, final ServiceVerificationHandler serviceVerificationHandler) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        if (rarName.contains(ConnectorServices.RA_SERVICE_NAME_SEPARATOR)) {
            rarName = rarName.substring(0, rarName.indexOf(ConnectorServices.RA_SERVICE_NAME_SEPARATOR));
        }
        final ServiceController<?> inactiveRaController = registry.getService(ConnectorServices.INACTIVE_RESOURCE_ADAPTER_SERVICE.append(rarName));
        if (inactiveRaController == null) {
            throw new OperationFailedException("rar not yet deployed");
        }
        InactiveResourceAdapterDeploymentService.InactiveResourceAdapterDeployment inactive = (InactiveResourceAdapterDeploymentService.InactiveResourceAdapterDeployment) inactiveRaController.getValue();
        final ServiceController<?> RaxmlController = registry.getService(ServiceName.of(ConnectorServices.RA_SERVICE, raName));

        ResourceAdapter raxml = (ResourceAdapter) RaxmlController.getValue();
        RaServicesFactory.createDeploymentService(inactive.getRegistration(), inactive.getConnectorXmlDescriptor(), inactive.getModule(), inactive.getServiceTarget(), raName, inactive.getDeploymentUnitServiceName(), inactive.getDeployment(), raxml, inactive.getResource(), serviceVerificationHandler);
    }

    public static ServiceName installRaServices(OperationContext context, ServiceVerificationHandler verificationHandler, String name, ModifiableResourceAdapter resourceAdapter) {
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ServiceController<?> resourceAdaptersService = context.getServiceRegistry(false).getService(
                ConnectorServices.RESOURCEADAPTERS_SERVICE);
        ServiceController<?> controller = null;
        if (resourceAdaptersService == null) {
            controller = serviceTarget.addService(ConnectorServices.RESOURCEADAPTERS_SERVICE,
                    new ResourceAdaptersService()).setInitialMode(ServiceController.Mode.ACTIVE).addListener(verificationHandler).install();
        }
        ServiceName raServiceName = ServiceName.of(ConnectorServices.RA_SERVICE, name);
        String bootStrapCtxName = DEFAULT_NAME;
        if (resourceAdapter.getBootstrapContext() != null && ! resourceAdapter.getBootstrapContext().equals("undefined")) {
            bootStrapCtxName = resourceAdapter.getBootstrapContext();
        }
        ResourceAdapterService raService = new ResourceAdapterService(resourceAdapter);
        serviceTarget.addService(raServiceName, raService).setInitialMode(ServiceController.Mode.ACTIVE)
                .addDependency(ConnectorServices.RESOURCEADAPTERS_SERVICE, ResourceAdaptersService.ModifiableResourceAdaptors.class, raService.getResourceAdaptersInjector())
                .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(bootStrapCtxName))
                .addListener(verificationHandler).install();
        return raServiceName;
    }

    public static void installRaServicesAndDeployFromModule(OperationContext context, ServiceVerificationHandler verificationHandler, String name, ModifiableResourceAdapter resourceAdapter, String moduleName) throws OperationFailedException{
        ServiceName raServiceName =  installRaServices(context, verificationHandler, name, resourceAdapter);
        final boolean resolveProperties = true;
        final ServiceTarget serviceTarget = context.getServiceTarget();
        String deploymentName = moduleName;//.substring(0, deploymentRoot.getName().indexOf(".rar"));

        if (moduleName.contains("->")) {
            moduleName = moduleName.substring(0, moduleName.indexOf("->"));
        }
        //load module
        String slot = "main";
        if (moduleName.contains(":")) {
            slot = moduleName.substring(moduleName.indexOf(":") + 1);
            moduleName = moduleName.substring(0, moduleName.indexOf(":"));
        }

        Module module;
        try {
            ModuleIdentifier moduleId = ModuleIdentifier.create(moduleName, slot);
            module = Module.getCallerModuleLoader().loadModule(moduleId);
        } catch (ModuleLoadException e) {
            throw new OperationFailedException(MESSAGES.failedToLoadModuleRA(moduleName), e);
        }
        URL path = module.getExportedResource("META-INF/ra.xml");
        Closeable closable = null;
            try {
                VirtualFile child;
                if (path.getPath().contains("!")) {
                    child = VFS.getChild(path.getPath().split("!")[0].split("file:")[1]);

                    closable = VFS.mountZip(new File(path.getPath().split("!")[0].split("file:")[1]), child, TempFileProviderService.provider());
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
                ConnectorXmlDescriptor connectorXmlDescriptor = RaDeploymentParsingProcessor.process(resolveProperties, deploymentRoot, null, deploymentName);
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
                ServiceBuilder builder = ParsedRaDeploymentProcessor.process(connectorXmlDescriptor, ironJacamarXmlDescriptor, module.getClassLoader(), serviceTarget, annotationIndexes, RAR_MODULE.append(deploymentName), verificationHandler);
                builder.addDependency(raServiceName).setInitialMode(ServiceController.Mode.ACTIVE).install();
                String rarName = resourceAdapter.getArchive();
                Integer identifier = null;
                if (rarName.contains(ConnectorServices.RA_SERVICE_NAME_SEPARATOR)) {
                    rarName = rarName.substring(0, rarName.indexOf(ConnectorServices.RA_SERVICE_NAME_SEPARATOR));
                }
                if (deploymentName.equals(rarName)) {
                    RaServicesFactory.createDeploymentService(connectorXmlDescriptor, module, serviceTarget, deploymentName, RAR_MODULE.append(deploymentName), resourceAdapter, verificationHandler);


                }

            } catch (Exception e) {
                throw new OperationFailedException(MESSAGES.failedToLoadModuleRA(moduleName), e);
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
