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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTIONSUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.metadata.deployment.InactiveResourceAdapterDeploymentService;
import org.jboss.as.connector.util.RaServicesFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
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
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

public class RaOperationUtil {

    public static ModifiableResourceAdapter buildResourceAdaptersObject(ModelNode operation) throws OperationFailedException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
        List<CommonConnDef> connectionDefinitions = new ArrayList<CommonConnDef>(0);
        List<CommonAdminObject> adminObjects = new ArrayList<CommonAdminObject>(0);
//        if (operation.hasDefined(CONFIG_PROPERTIES.getName())) {
//            configProperties = new HashMap<String, String>(operation.get(CONFIG_PROPERTIES.getName()).asList().size());
//            for (ModelNode property : operation.get(CONFIG_PROPERTIES.getName()).asList()) {
//                configProperties.put(property.asProperty().getName(), property.asProperty().getValue().asString());
//            }
//        }
        String archive = getStringIfSetOrGetDefault(operation, ARCHIVE.getName(), null);
        TransactionSupportEnum transactionSupport = operation.hasDefined(TRANSACTIONSUPPORT.getName()) ? TransactionSupportEnum
                .valueOf(operation.get(TRANSACTIONSUPPORT.getName()).asString()) : null;
        String bootstrapContext = getStringIfSetOrGetDefault(operation, BOOTSTRAPCONTEXT.getName(), null);
        List<String> beanValidationGroups = null;
        if (operation.hasDefined(BEANVALIDATIONGROUPS.getName())) {
            beanValidationGroups = new ArrayList<String>(operation.get(BEANVALIDATIONGROUPS.getName()).asList().size());
            for (ModelNode beanValidtion : operation.get(BEANVALIDATIONGROUPS.getName()).asList()) {
                beanValidationGroups.add(beanValidtion.asString());
            }

        }
        ModifiableResourceAdapter ra;
        ra = new ModifiableResourceAdapter(archive, transactionSupport, connectionDefinitions,
                adminObjects, configProperties, beanValidationGroups, bootstrapContext);

        return ra;

    }

    public static ModifiableConnDef buildConnectionDefinitionObject(final OperationContext context, final ModelNode operation, final String poolName) throws OperationFailedException, ValidateException {
        Map<String, String> configProperties = new HashMap<String, String>(0);
//        if (operation.hasDefined(CONFIG_PROPERTIES.getName())) {
//            configProperties = new HashMap<String, String>(operation.get(CONFIG_PROPERTIES.getName()).asList().size());
//            for (ModelNode property : operation.get(CONFIG_PROPERTIES.getName()).asList()) {
//                configProperties.put(property.asProperty().getName(), property.asProperty().getValue().asString());
//            }
//        }
        String className = getStringIfSetOrGetDefault(operation, CLASS_NAME.getName(), null);
        String jndiName = getStringIfSetOrGetDefault(operation, JNDINAME.getName(), null);
        boolean enabled = getBooleanIfSetOrGetDefault(operation, ENABLED.getName(), Defaults.ENABLED);
        boolean useJavaContext = getBooleanIfSetOrGetDefault(operation, USE_JAVA_CONTEXT.getName(), Defaults.USE_JAVA_CONTEXT);
        boolean useCcm = getBooleanIfSetOrGetDefault(operation, USE_CCM.getName(), Defaults.USE_CCM);

        Integer maxPoolSize = getIntIfSetOrGetDefault(operation, MAX_POOL_SIZE.getName(), Defaults.MAX_POOL_SIZE);
        Integer minPoolSize = getIntIfSetOrGetDefault(operation, MIN_POOL_SIZE.getName(), Defaults.MIN_POOL_SIZE);
        boolean prefill = getBooleanIfSetOrGetDefault(operation, POOL_PREFILL.getName(), Defaults.PREFILL);
        boolean useStrictMin = getBooleanIfSetOrGetDefault(operation, POOL_USE_STRICT_MIN.getName(), Defaults.USE_STRICT_MIN);
        final FlushStrategy flushStrategy = operation.hasDefined(POOL_FLUSH_STRATEGY.getName()) ? FlushStrategy.forName(operation
                .get(POOL_FLUSH_STRATEGY.getName()).asString()) : Defaults.FLUSH_STRATEGY;

        Integer allocationRetry = getIntIfSetOrGetDefault(operation, ALLOCATION_RETRY.getName(), null);
        Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(operation, ALLOCATION_RETRY_WAIT_MILLIS.getName(), null);
        Long blockingTimeoutMillis = getLongIfSetOrGetDefault(operation, BLOCKING_TIMEOUT_WAIT_MILLIS.getName(), null);
        Long idleTimeoutMinutes = getLongIfSetOrGetDefault(operation, IDLETIMEOUTMINUTES.getName(), null);
        Integer xaResourceTimeout = getIntIfSetOrGetDefault(operation, XA_RESOURCE_TIMEOUT.getName(), null);
        CommonTimeOut timeOut = new CommonTimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout);
        CommonPool pool = new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy);

        String securityDomain = getStringIfSetOrGetDefault(operation, SECURITY_DOMAIN.getName(), null);
        String securityDomainAndApplication = getStringIfSetOrGetDefault(operation, SECURITY_DOMAIN_AND_APPLICATION.getName(),
                null);
        Boolean application = getBooleanIfSetOrGetDefault(operation, APPLICATION.getName(), null);

        CommonSecurity security = null;

        if (securityDomain != null && securityDomainAndApplication != null && application != null) {
            if (application == null) application = Defaults.APPLICATION_MANAGED_SECURITY;
            security = new CommonSecurityImpl(securityDomain, securityDomainAndApplication, application);
        }


        Long backgroundValidationMillis = getLongIfSetOrGetDefault(operation, BACKGROUNDVALIDATIONMILLIS.getName(), null);
        boolean backgroundValidation = getBooleanIfSetOrGetDefault(operation, BACKGROUNDVALIDATION.getName(), Defaults.BACKGROUND_VALIDATION);
        boolean useFastFail = getBooleanIfSetOrGetDefault(operation, USE_FAST_FAIL.getName(), Defaults.USE_FAST_FAIl);
        CommonValidation validation = new CommonValidationImpl(backgroundValidation, backgroundValidationMillis,
                useFastFail);
        final String recoveryUsername = getStringIfSetOrGetDefault(operation, RECOVERY_USERNAME.getName(), null);
        //TODO This will be cleaned up once it uses attribute definitions
        String recoveryPassword = getResolvedStringIfSetOrGetDefault(context, operation, RECOVERY_PASSWORD.getName(), null);
        final String recoverySecurityDomain = getStringIfSetOrGetDefault(operation, RECOVERY_SECURITY_DOMAIN.getName(), null);
        Boolean noRecovery = getBooleanIfSetOrGetDefault(operation, NO_RECOVERY.getName(), null);

        Recovery recovery = null;
        if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null || noRecovery != null) {
            Credential credential = null;

            if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null)
                credential = new CredentialImpl(recoveryUsername, recoveryPassword, recoverySecurityDomain);

            Extension recoverPlugin = extractExtension(operation, RECOVERLUGIN_CLASSNAME.getName(), RECOVERLUGIN_PROPERTIES.getName());

            if (noRecovery == null)
                noRecovery = Boolean.FALSE;

            recovery = new Recovery(credential, recoverPlugin, noRecovery);
        }
        ModifiableConnDef connectionDefinition = new ModifiableConnDef(configProperties, className, jndiName, poolName,
                enabled, useJavaContext, useCcm, pool, timeOut, validation, security, recovery);

        return connectionDefinition;

    }

    public static ModifiableAdminObject buildAdminObjects(ModelNode operation, final String poolName) {
                Map<String, String> configProperties = new HashMap<String, String>(0);
                String className = getStringIfSetOrGetDefault(operation, CLASS_NAME.getName(), null);
                String jndiName = getStringIfSetOrGetDefault(operation, JNDINAME.getName(), null);
                boolean enabled = getBooleanIfSetOrGetDefault(operation, ENABLED.getName(), Defaults.ENABLED);
                boolean useJavaContext = getBooleanIfSetOrGetDefault(operation, USE_JAVA_CONTEXT.getName(), Defaults.USE_JAVA_CONTEXT);

                ModifiableAdminObject adminObjet = new ModifiableAdminObject(configProperties, className, jndiName, poolName,
                        enabled, useJavaContext);

                return adminObjet;
    }

    private static Long getLongIfSetOrGetDefault(ModelNode dataSourceNode, String key, Long defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asLong();
        } else {
            return defaultValue;
        }
    }

    private static Integer getIntIfSetOrGetDefault(ModelNode dataSourceNode, String key, Integer defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asInt();
        } else {
            return defaultValue;
        }
    }

    private static Boolean getBooleanIfSetOrGetDefault(ModelNode dataSourceNode, String key, Boolean defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asBoolean();
        } else {
            return defaultValue;
        }
    }

    private static String getStringIfSetOrGetDefault(ModelNode dataSourceNode, String key, String defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asString();
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

    private static Extension extractExtension(final ModelNode node, final String className, final String propertyName)
            throws ValidateException {
        if (node.hasDefined(className)) {
            String exceptionSorterClassName = node.get(className).asString();

            getStringIfSetOrGetDefault(node, className, null);

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
        if (raDeploymentServiceName != null)  {
            context.removeService(raDeploymentServiceName);
            ConnectorServices.unregisterDeployment(raName, raDeploymentServiceName);
        }
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
