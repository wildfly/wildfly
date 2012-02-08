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

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.metadata.deployment.InactiveResourceAdapterDeploymentService;
import org.jboss.as.connector.util.RaServicesFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAPCONTEXT;
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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTIONSUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;

public class RaOperationUtil {

    public static ModifiableResourceAdapter buildResourceAdaptersObject(final OperationContext context,ModelNode operation) throws OperationFailedException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        List<CommonConnDef> connectionDefinitions = new ArrayList<CommonConnDef>(0);
        List<CommonAdminObject> adminObjects = new ArrayList<CommonAdminObject>(0);
//        if (operation.hasDefined(CONFIG_PROPERTIES.getName())) {
//            configProperties = new HashMap<String, String>(operation.get(CONFIG_PROPERTIES.getName()).asList().size());
//            for (ModelNode property : operation.get(CONFIG_PROPERTIES.getName()).asList()) {
//                configProperties.put(property.asProperty().getName(), property.asProperty().getValue().asString());
//            }
//        }
        String archive = getResolvedStringIfSetOrGetDefault(context, operation, ARCHIVE.getName(), null);
        TransactionSupportEnum transactionSupport = operation.hasDefined(TRANSACTIONSUPPORT.getName()) ? TransactionSupportEnum
                .valueOf(operation.get(TRANSACTIONSUPPORT.getName()).asString()) : null;
        String bootstrapContext = getResolvedStringIfSetOrGetDefault(context, operation, BOOTSTRAPCONTEXT.getName(), null);
        List<String> beanValidationGroups = null;
        if (operation.hasDefined(BEANVALIDATIONGROUPS.getName())) {
            beanValidationGroups = new ArrayList<String>(operation.get(BEANVALIDATIONGROUPS.getName()).asList().size());
            for (ModelNode beanValidation : operation.get(BEANVALIDATIONGROUPS.getName()).asList()) {
                beanValidationGroups.add(beanValidation.asString());
            }

        }
        ModifiableResourceAdapter ra;
        ra = new ModifiableResourceAdapter(archive, transactionSupport, connectionDefinitions,
                adminObjects, configProperties, beanValidationGroups, bootstrapContext);

        return ra;

    }

    public static ModifiableConnDef buildConnectionDefinitionObject(final OperationContext context,
                                                                    final ModelNode operation, final String poolName,
                                                                    final boolean isXa) throws OperationFailedException, ValidateException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
//        if (operation.hasDefined(CONFIG_PROPERTIES.getName())) {
//            configProperties = new HashMap<String, String>(operation.get(CONFIG_PROPERTIES.getName()).asList().size());
//            for (ModelNode property : operation.get(CONFIG_PROPERTIES.getName()).asList()) {
//                configProperties.put(property.asProperty().getName(), property.asProperty().getValue().asString());
//            }
//        }
        String className = getResolvedStringIfSetOrGetDefault(context, operation, CLASS_NAME.getName(), null);
        String jndiName = getResolvedStringIfSetOrGetDefault(context, operation, JNDINAME.getName(), null);
        boolean enabled = getBooleanIfSetOrGetDefault(context, operation, ENABLED, Defaults.ENABLED);
        boolean useJavaContext = getBooleanIfSetOrGetDefault(context, operation, USE_JAVA_CONTEXT, Defaults.USE_JAVA_CONTEXT);
        boolean useCcm = getBooleanIfSetOrGetDefault(context, operation, USE_CCM, Defaults.USE_CCM);

        Integer maxPoolSize = getIntIfSetOrGetDefault(context, operation, MAX_POOL_SIZE, Defaults.MAX_POOL_SIZE);
        Integer minPoolSize = getIntIfSetOrGetDefault(context, operation, MIN_POOL_SIZE, Defaults.MIN_POOL_SIZE);
        boolean prefill = getBooleanIfSetOrGetDefault(context, operation, POOL_PREFILL, Defaults.PREFILL);
        boolean useStrictMin = getBooleanIfSetOrGetDefault(context, operation, POOL_USE_STRICT_MIN, Defaults.USE_STRICT_MIN);
        final FlushStrategy flushStrategy = operation.hasDefined(POOL_FLUSH_STRATEGY.getName()) ? FlushStrategy.forName(operation
                .get(POOL_FLUSH_STRATEGY.getName()).asString()) : Defaults.FLUSH_STRATEGY;
        Boolean isSameRM = getBooleanIfSetOrGetDefault(context, operation, SAME_RM_OVERRIDE, Defaults.IS_SAME_RM_OVERRIDE);
        Boolean interlivng = getBooleanIfSetOrGetDefault(context, operation, INTERLEAVING, Defaults.INTERLEAVING);
        Boolean padXid = getBooleanIfSetOrGetDefault(context, operation, PAD_XID, Defaults.PAD_XID);
        Boolean wrapXaResource = getBooleanIfSetOrGetDefault(context, operation, WRAP_XA_RESOURCE, Defaults.WRAP_XA_RESOURCE);
        Boolean noTxSeparatePool = getBooleanIfSetOrGetDefault(context, operation, NOTXSEPARATEPOOL, Defaults.NO_TX_SEPARATE_POOL);

        Integer allocationRetry = getIntIfSetOrGetDefault(context, operation, ALLOCATION_RETRY, null);
        Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(context, operation, ALLOCATION_RETRY_WAIT_MILLIS, null);
        Long blockingTimeoutMillis = getLongIfSetOrGetDefault(context, operation, BLOCKING_TIMEOUT_WAIT_MILLIS, null);
        Long idleTimeoutMinutes = getLongIfSetOrGetDefault(context, operation, IDLETIMEOUTMINUTES, null);
        Integer xaResourceTimeout = getIntIfSetOrGetDefault(context, operation, XA_RESOURCE_TIMEOUT, null);

        CommonTimeOut timeOut = new CommonTimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout);
        CommonPool pool = null;
        if (isXa) {
              pool = new CommonXaPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy, isSameRM,interlivng, padXid, wrapXaResource, noTxSeparatePool);
        }   else {
              pool =  new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy);
        }
        String securityDomain = getResolvedStringIfSetOrGetDefault(context, operation, SECURITY_DOMAIN.getName(), null);
        String securityDomainAndApplication = getResolvedStringIfSetOrGetDefault(context, operation, SECURITY_DOMAIN_AND_APPLICATION.getName(),
                null);
        Boolean application = getBooleanIfSetOrGetDefault(context, operation, APPLICATION, null);
        CommonSecurity security = null;
        if (securityDomain != null || securityDomainAndApplication != null || application != null) {
            if (application == null)
                application = Defaults.APPLICATION_MANAGED_SECURITY;

            security = new CommonSecurityImpl(securityDomain, securityDomainAndApplication, application);
        }
        Long backgroundValidationMillis = getLongIfSetOrGetDefault(context, operation, BACKGROUNDVALIDATIONMILLIS, null);
        boolean backgroundValidation = getBooleanIfSetOrGetDefault(context, operation, BACKGROUNDVALIDATION, Defaults.BACKGROUND_VALIDATION);
        boolean useFastFail = getBooleanIfSetOrGetDefault(context, operation, USE_FAST_FAIL, Defaults.USE_FAST_FAIl);
        CommonValidation validation = new CommonValidationImpl(backgroundValidation, backgroundValidationMillis,
                useFastFail);
        final String recoveryUsername = getResolvedStringIfSetOrGetDefault(context, operation, RECOVERY_USERNAME.getName(), null);
        //TODO This will be cleaned up once it uses attribute definitions
        String recoveryPassword = getResolvedStringIfSetOrGetDefault(context, operation, RECOVERY_PASSWORD.getName(), null);
        final String recoverySecurityDomain = getResolvedStringIfSetOrGetDefault(context, operation, RECOVERY_SECURITY_DOMAIN.getName(), null);
        Boolean noRecovery = getBooleanIfSetOrGetDefault(context, operation, NO_RECOVERY, null);

        Recovery recovery = null;
        if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null || noRecovery != null) {
            Credential credential = null;

            if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null)
                credential = new CredentialImpl(recoveryUsername, recoveryPassword, recoverySecurityDomain);

            Extension recoverPlugin = extractExtension(context, operation, RECOVERLUGIN_CLASSNAME.getName(), RECOVERLUGIN_PROPERTIES.getName());

            if (noRecovery == null)
                noRecovery = Boolean.FALSE;

            recovery = new Recovery(credential, recoverPlugin, noRecovery);
        }
        ModifiableConnDef connectionDefinition = new ModifiableConnDef(configProperties, className, jndiName, poolName,
                enabled, useJavaContext, useCcm, pool, timeOut, validation, security, recovery);

        return connectionDefinition;

    }

    public static ModifiableAdminObject buildAdminObjects(final OperationContext operationContext, ModelNode operation, final String poolName) throws OperationFailedException, ValidateException {
                Map<String, String> configProperties = new HashMap<String, String>(0);
                String className = getResolvedStringIfSetOrGetDefault(operationContext, operation, CLASS_NAME.getName(), null);
                String jndiName = getResolvedStringIfSetOrGetDefault(operationContext, operation, JNDINAME.getName(), null);
                boolean enabled = getBooleanIfSetOrGetDefault(operationContext, operation, ENABLED, Defaults.ENABLED);
                boolean useJavaContext = getBooleanIfSetOrGetDefault(operationContext, operation, USE_JAVA_CONTEXT, Defaults.USE_JAVA_CONTEXT);

                ModifiableAdminObject adminObject = new ModifiableAdminObject(configProperties, className, jndiName, poolName,
                        enabled, useJavaContext);

                return adminObject;
    }

    private static Long getLongIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key, final Long defaultValue) throws OperationFailedException {
            if (dataSourceNode.hasDefined(key.getName())) {
                if (key.isAllowExpression()) {
                    return context.resolveExpressions(dataSourceNode.get(key.getName())).asLong();
                } else {
                    return dataSourceNode.get(key.getName()).asLong();
                }
            } else {
                return defaultValue;
            }
        }

        private static Integer getIntIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key, final Integer defaultValue) throws OperationFailedException {
            if (dataSourceNode.hasDefined(key.getName())) {
                if (key.isAllowExpression()) {
                    return context.resolveExpressions(dataSourceNode.get(key.getName())).asInt();
                } else {
                    return dataSourceNode.get(key.getName()).asInt();
                }
            } else {
                return defaultValue;
            }
        }

        private static Boolean getBooleanIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key,
                final Boolean defaultValue) throws OperationFailedException {
            if (dataSourceNode.hasDefined(key.getName())) {
                if (key.isAllowExpression()) {
                    return context.resolveExpressions(dataSourceNode.get(key.getName())).asBoolean();
                } else {
                    return dataSourceNode.get(key.getName()).asBoolean();
                }
            } else {
                return defaultValue;
            }
        }


    private static String getResolvedStringIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final String key, final String defaultValue) throws OperationFailedException {
        if (dataSourceNode.hasDefined(key)) {
            return context.resolveExpressions(dataSourceNode.get(key)).asString();
        } else {
            return defaultValue;
        }
    }

    private static Extension extractExtension(final OperationContext operationContext, final ModelNode node, final String className, final String propertyName)
            throws ValidateException, OperationFailedException {
        if (node.hasDefined(className)) {
            String exceptionSorterClassName = node.get(className).asString();

            getResolvedStringIfSetOrGetDefault(operationContext, node, className, null);

            Map<String, String> exceptionSorterProperty = null;
            if (node.hasDefined(propertyName)) {
                exceptionSorterProperty = new HashMap<String, String>(node.get(propertyName).asList().size());
                for (ModelNode property : node.get(propertyName).asList()) {
                    exceptionSorterProperty.put(property.asProperty().getName(), property.asProperty().getValue().asString());
                }
            }

            return new Extension(exceptionSorterClassName, exceptionSorterProperty);
        } else {
            return null;
        }
    }

    public static void deactivateIfActive(OperationContext context, String raName) throws OperationFailedException {
        final ServiceName raDeploymentServiceName = ConnectorServices.getDeploymentServiceName(raName);
        Integer identifier = 0;
        if (raName.indexOf("->") != -1) {
            identifier = Integer.valueOf(raName.substring(raName.indexOf("->")+2));
            raName = raName.substring(0,raName.indexOf("->"));
        }
        if (raDeploymentServiceName != null)  {
            context.removeService(raDeploymentServiceName);
            ConnectorServices.unregisterDeployment(raName, raDeploymentServiceName);
        }
        ConnectorServices.unregisterResourceIdentifier(raName, identifier);

    }

    public static void activate(OperationContext context, String raName, String rarName)  throws OperationFailedException {
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

        RaServicesFactory.createDeploymentService(inactive.getRegistration(), inactive.getConnectorXmlDescriptor(), inactive.getModule(), context.getServiceTarget(), inactive.getDeployment(), inactive.getDeployment(), raxml);
    }
}
