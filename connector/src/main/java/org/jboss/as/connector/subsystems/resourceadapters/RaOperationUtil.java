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
import static org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.connector.services.resourceadapters.deployment.AbstractResourceAdapterDeploymentService;
import org.jboss.as.connector.services.resourceadapters.deployment.InactiveResourceAdapterDeploymentService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.RaServicesFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.dmr.ModelNode;
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
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

public class RaOperationUtil {

    public static ModifiableResourceAdapter buildResourceAdaptersObject(final OperationContext context, ModelNode operation) throws OperationFailedException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        List<CommonConnDef> connectionDefinitions = new ArrayList<CommonConnDef>(0);
        List<CommonAdminObject> adminObjects = new ArrayList<CommonAdminObject>(0);
        String archive = ARCHIVE.resolveModelAttribute(context, operation).asString();
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
        ra = new ModifiableResourceAdapter(archive, transactionSupport, connectionDefinitions,
                adminObjects, configProperties, beanValidationGroups, bootstrapContext);

        return ra;

    }

    public static ModifiableConnDef buildConnectionDefinitionObject(final OperationContext context, final ModelNode operation, final String poolName,
                                                                    final boolean isXa) throws OperationFailedException, ValidateException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        String className = CLASS_NAME.resolveModelAttribute(context, operation).asString();
        String jndiName = JNDINAME.resolveModelAttribute(context, operation).asString();
        boolean enabled = ENABLED.resolveModelAttribute(context, operation).asBoolean();
        boolean useJavaContext = USE_JAVA_CONTEXT.resolveModelAttribute(context, operation).asBoolean();
        boolean useCcm = USE_CCM.resolveModelAttribute(context, operation).asBoolean();

        int maxPoolSize = MAX_POOL_SIZE.resolveModelAttribute(context, operation).asInt();
        int minPoolSize = MIN_POOL_SIZE.resolveModelAttribute(context, operation).asInt();
        boolean prefill = POOL_PREFILL.resolveModelAttribute(context, operation).asBoolean();
        boolean useStrictMin = POOL_USE_STRICT_MIN.resolveModelAttribute(context, operation).asBoolean();
        String flushStrategyString = POOL_FLUSH_STRATEGY.resolveModelAttribute(context, operation).asString();
        final FlushStrategy flushStrategy = FlushStrategy.forName(flushStrategyString);
        boolean isSameRM = SAME_RM_OVERRIDE.resolveModelAttribute(context, operation).asBoolean();
        boolean interlivng = INTERLEAVING.resolveModelAttribute(context, operation).asBoolean();
        boolean padXid = PAD_XID.resolveModelAttribute(context, operation).asBoolean();
        boolean wrapXaResource = WRAP_XA_RESOURCE.resolveModelAttribute(context, operation).asBoolean();
        boolean noTxSeparatePool = NOTXSEPARATEPOOL.resolveModelAttribute(context, operation).asBoolean();

        ModelNode allocationRetryModel = ALLOCATION_RETRY.resolveModelAttribute(context, operation);
        ModelNode allocationRetryWaitMillisModel = ALLOCATION_RETRY_WAIT_MILLIS.resolveModelAttribute(context, operation);
        ModelNode blockingTimeoutMillisModel = BLOCKING_TIMEOUT_WAIT_MILLIS.resolveModelAttribute(context, operation);
        ModelNode idleTimeoutMinutesModel = IDLETIMEOUTMINUTES.resolveModelAttribute(context, operation);
        ModelNode xaResourceTimeoutModel = XA_RESOURCE_TIMEOUT.resolveModelAttribute(context, operation);


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
        ModelNode securityDomainModel = SECURITY_DOMAIN.resolveModelAttribute(context, operation);
        String securityDomain = securityDomainModel.isDefined()?securityDomainModel.asString():null;
        ModelNode securityDomainAndApplicationModel = SECURITY_DOMAIN_AND_APPLICATION.resolveModelAttribute(context, operation);
        String securityDomainAndApplication = securityDomainAndApplicationModel.isDefined()?securityDomainAndApplicationModel.asString():null;

        boolean application = APPLICATION.resolveModelAttribute(context, operation).asBoolean();
        CommonSecurity security = null;
        if (securityDomain != null || securityDomainAndApplication != null) {
            security = new CommonSecurityImpl(securityDomain, securityDomainAndApplication, application);
        }
        ModelNode backgroundValidationMillisModel = BACKGROUNDVALIDATIONMILLIS.resolveModelAttribute(context, operation);
        Long backgroundValidationMillis = backgroundValidationMillisModel.isDefined()?backgroundValidationMillisModel.asLong():null;
        boolean backgroundValidation = BACKGROUNDVALIDATION.resolveModelAttribute(context, operation).asBoolean();
        boolean useFastFail = USE_FAST_FAIL.resolveModelAttribute(context, operation).asBoolean();
        CommonValidation validation = new CommonValidationImpl(backgroundValidation, backgroundValidationMillis, useFastFail);
        final String recoveryUsername = RECOVERY_USERNAME.resolveModelAttribute(context, operation).asString();

        final ModelNode recoveryPasswordModel = RECOVERY_PASSWORD.resolveModelAttribute(context, operation);
        final String recoveryPassword = recoveryPasswordModel.isDefined()?recoveryPasswordModel.asString():null;
        final ModelNode recoverySecurityDomainModel = RECOVERY_SECURITY_DOMAIN.resolveModelAttribute(context, operation);
        final String recoverySecurityDomain = recoverySecurityDomainModel.isDefined()?recoverySecurityDomainModel.asString():null;
        boolean noRecovery = NO_RECOVERY.resolveModelAttribute(context, operation).asBoolean();

        Recovery recovery = null;
        if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null) {
            Credential credential = null;
            credential = new CredentialImpl(recoveryUsername, recoveryPassword, recoverySecurityDomain);
            Extension recoverPlugin = extractExtension(context, operation, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);
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

    private static Extension extractExtension(final OperationContext operationContext, final ModelNode node, final SimpleAttributeDefinition className, final SimpleMapAttributeDefinition propertyName)
            throws ValidateException, OperationFailedException {
        if (node.hasDefined(className.getName())) {
            String exceptionSorterClassName = className.resolveModelAttribute(operationContext, node).asString();

            Map<String, String> exceptionSorterProperty = null;
            if (node.hasDefined(propertyName.getName())) {
                exceptionSorterProperty = new HashMap<String, String>(node.get(propertyName.getName()).asList().size());
                for (ModelNode property : node.get(propertyName.getName()).asList()) {
                    exceptionSorterProperty.put(property.asProperty().getName(), property.asProperty().getValue().asString());
                }
            }

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
        AbstractResourceAdapterDeploymentService service = ((AbstractResourceAdapterDeploymentService) context.getServiceRegistry(false).getService(deploymentServiceName));

        return wasActive;

    }

    public static void activate(OperationContext context, String raName, String rarName) throws OperationFailedException {
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
        RaServicesFactory.createDeploymentService(inactive.getRegistration(), inactive.getConnectorXmlDescriptor(), inactive.getModule(), inactive.getServiceTarget(), raName, inactive.getDeployment(), raxml, inactive.getResource());
    }

    public static void installRaServices(OperationContext context, ServiceVerificationHandler verificationHandler, String name, ModifiableResourceAdapter resourceAdapter) {
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ServiceController<?> resourceAdaptersService = context.getServiceRegistry(false).getService(
                ConnectorServices.RESOURCEADAPTERS_SERVICE);
        ServiceController<?> controller = null;
        if (resourceAdaptersService == null) {
            controller = serviceTarget.addService(ConnectorServices.RESOURCEADAPTERS_SERVICE,
                    new ResourceAdaptersService()).setInitialMode(ServiceController.Mode.ACTIVE).addListener(verificationHandler).install();
        }
        ServiceName raServiceName = ServiceName.of(ConnectorServices.RA_SERVICE, name);

        ResourceAdapterService raService = new ResourceAdapterService(resourceAdapter);
        serviceTarget.addService(raServiceName, raService).setInitialMode(ServiceController.Mode.ACTIVE)
                .addDependency(ConnectorServices.RESOURCEADAPTERS_SERVICE, ResourceAdaptersService.ModifiableResourceAdaptors.class, raService.getResourceAdaptersInjector())
                .addListener(verificationHandler).install();
    }
}
