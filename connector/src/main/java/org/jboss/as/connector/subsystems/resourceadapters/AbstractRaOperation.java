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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAPCONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_NAME;
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

import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersService.ModifiableResourceAdaptors;
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
import org.jboss.jca.common.metadata.common.CommonAdminObjectImpl;
import org.jboss.jca.common.metadata.common.CommonConnDefImpl;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonSecurityImpl;
import org.jboss.jca.common.metadata.common.CommonTimeOutImpl;
import org.jboss.jca.common.metadata.common.CommonValidationImpl;
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.jca.common.metadata.resourceadapter.ResourceAdapterImpl;

public abstract class AbstractRaOperation {

    protected ModifiableResourceAdaptors buildResourceAdaptersObject(ModelNode operation) throws OperationFailedException {
        List<ResourceAdapter> resourceAdapters = new ArrayList<ResourceAdapter>();
        Map<String, String> configProperties = null;
        if (operation.hasDefined(CONFIG_PROPERTIES.getName())) {
            configProperties = new HashMap<String, String>(operation.get(CONFIG_PROPERTIES.getName()).asList().size());
            for (ModelNode property : operation.get(CONFIG_PROPERTIES.getName()).asList()) {
                configProperties.put(property.asProperty().getName(), property.asProperty().getValue().asString());
            }
        }
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
        ResourceAdapter ra;
        try {
            ra = new ResourceAdapterImpl(archive, transactionSupport, buildConnectionDefinitionObject(operation),
                    buildAdminObjects(operation), configProperties, beanValidationGroups, bootstrapContext);
        } catch (ValidateException e) {
            throw new OperationFailedException(e, operation);
        }

        resourceAdapters.add(ra);

        return new ModifiableResourceAdaptors(resourceAdapters);

    }

    private List<CommonConnDef> buildConnectionDefinitionObject(ModelNode parentNode) throws ValidateException {
        List<CommonConnDef> connDefs = new ArrayList<CommonConnDef>();
        if (parentNode.hasDefined(CONNECTIONDEFINITIONS_NAME)) {
            for (ModelNode conDefNode : parentNode.get(CONNECTIONDEFINITIONS_NAME).asList()) {
                Map<String, String> configProperties = null;
                if (conDefNode.hasDefined(CONFIG_PROPERTIES.getName())) {
                    configProperties = new HashMap<String, String>(conDefNode.get(CONFIG_PROPERTIES.getName()).asList().size());
                    for (ModelNode property : conDefNode.get(CONFIG_PROPERTIES.getName()).asList()) {
                        configProperties.put(property.asProperty().getName(), property.asProperty().getValue().asString());
                    }
                }
                String className = getStringIfSetOrGetDefault(conDefNode, CLASS_NAME.getName(), null);
                String jndiName = getStringIfSetOrGetDefault(conDefNode, JNDINAME.getName(), null);
                String poolName = getStringIfSetOrGetDefault(conDefNode, POOL_NAME.getName(), null);
                boolean enabled = getBooleanIfSetOrGetDefault(conDefNode, ENABLED.getName(), Defaults.ENABLED);
                boolean useJavaContext = getBooleanIfSetOrGetDefault(conDefNode, USE_JAVA_CONTEXT.getName(), Defaults.USE_JAVA_CONTEXT);
                boolean useCcm = getBooleanIfSetOrGetDefault(conDefNode, USE_CCM.getName(), Defaults.USE_CCM);

                Integer maxPoolSize = getIntIfSetOrGetDefault(conDefNode, MAX_POOL_SIZE.getName(), Defaults.MAX_POOL_SIZE);
                Integer minPoolSize = getIntIfSetOrGetDefault(conDefNode, MIN_POOL_SIZE.getName(), Defaults.MIN_POOL_SIZE);
                boolean prefill = getBooleanIfSetOrGetDefault(conDefNode, POOL_PREFILL.getName(), Defaults.PREFILL);
                boolean useStrictMin = getBooleanIfSetOrGetDefault(conDefNode, POOL_USE_STRICT_MIN.getName(), Defaults.USE_STRICT_MIN);
                final FlushStrategy flushStrategy = conDefNode.hasDefined(POOL_FLUSH_STRATEGY.getName()) ? FlushStrategy.valueOf(conDefNode
                        .get(POOL_FLUSH_STRATEGY.getName()).asString()) : Defaults.FLUSH_STRATEGY;

                Integer allocationRetry = getIntIfSetOrGetDefault(conDefNode, ALLOCATION_RETRY.getName(), null);
                Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(conDefNode, ALLOCATION_RETRY_WAIT_MILLIS.getName(), null);
                Long blockingTimeoutMillis = getLongIfSetOrGetDefault(conDefNode, BLOCKING_TIMEOUT_WAIT_MILLIS.getName(), null);
                Long idleTimeoutMinutes = getLongIfSetOrGetDefault(conDefNode, IDLETIMEOUTMINUTES.getName(), null);
                Integer xaResourceTimeout = getIntIfSetOrGetDefault(conDefNode, XA_RESOURCE_TIMEOUT.getName(), null);
                CommonTimeOut timeOut = new CommonTimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                        allocationRetryWaitMillis, xaResourceTimeout);
                CommonPool pool = new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy);

                String securityDomain = getStringIfSetOrGetDefault(conDefNode, SECURITY_DOMAIN.getName(), null);
                String securityDomainAndApplication = getStringIfSetOrGetDefault(conDefNode, SECURITY_DOMAIN_AND_APPLICATION.getName(),
                        null);
                Boolean application = getBooleanIfSetOrGetDefault(conDefNode, APPLICATION.getName(), null);

                CommonSecurity security = null;

                if(securityDomain != null && securityDomainAndApplication != null && application != null) {
                    if (application == null) application = Defaults.APPLICATION_MANAGED_SECURITY;
                    security = new CommonSecurityImpl(securityDomain, securityDomainAndApplication, application);
                }


                Long backgroundValidationMillis = getLongIfSetOrGetDefault(conDefNode, BACKGROUNDVALIDATIONMILLIS.getName(), null);
                boolean backgroundValidation = getBooleanIfSetOrGetDefault(conDefNode, BACKGROUNDVALIDATION.getName(), Defaults.BACKGROUND_VALIDATION);
                boolean useFastFail = getBooleanIfSetOrGetDefault(conDefNode, USE_FAST_FAIL.getName(), Defaults.USE_FAST_FAIl);
                CommonValidation validation = new CommonValidationImpl(backgroundValidation, backgroundValidationMillis,
                        useFastFail);
                final String recoveryUsername = getStringIfSetOrGetDefault(conDefNode, RECOVERY_USERNAME.getName(), null);
                final String recoveryPassword = getStringIfSetOrGetDefault(conDefNode, RECOVERY_PASSWORD.getName(), null);
                final String recoverySecurityDomain = getStringIfSetOrGetDefault(conDefNode, RECOVERY_SECURITY_DOMAIN.getName(), null);

                final Credential credential = new CredentialImpl(recoveryUsername, recoveryPassword, recoverySecurityDomain);

                final Extension recoverPlugin = extractExtension(conDefNode, RECOVERLUGIN_CLASSNAME.getName(), RECOVERLUGIN_PROPERTIES.getName());
                final boolean noRecovery = getBooleanIfSetOrGetDefault(conDefNode, NO_RECOVERY.getName(), false);
                Recovery recovery = new Recovery(credential, recoverPlugin, noRecovery);
                CommonConnDef connectionDefinition = new CommonConnDefImpl(configProperties, className, jndiName, poolName,
                        enabled, useJavaContext, useCcm, pool, timeOut, validation, security, recovery);

                connDefs.add(connectionDefinition);
            }
        }
        return connDefs;
    }

    private List<CommonAdminObject> buildAdminObjects(ModelNode parentNode) {
        List<CommonAdminObject> adminObjets = new ArrayList<CommonAdminObject>();
        if (parentNode.hasDefined(ADMIN_OBJECTS_NAME)) {
            for (ModelNode adminObject : parentNode.get(ADMIN_OBJECTS_NAME).asList()) {
                Map<String, String> configProperties = null;
                if (adminObject.hasDefined(CONFIG_PROPERTIES.getName())) {
                    configProperties = new HashMap<String, String>(adminObject.get(CONFIG_PROPERTIES.getName()).asList().size());
                    for (ModelNode property : adminObject.get(CONFIG_PROPERTIES.getName()).asList()) {
                        configProperties.put(property.asProperty().getName(), property.asProperty().getValue().asString());
                    }
                }
                String className = getStringIfSetOrGetDefault(adminObject, CLASS_NAME.getName(), null);
                String jndiName = getStringIfSetOrGetDefault(adminObject, JNDINAME.getName(), null);
                String poolName = getStringIfSetOrGetDefault(adminObject, POOL_NAME.getName(), null);
                boolean enabled = getBooleanIfSetOrGetDefault(adminObject, ENABLED.getName(), Defaults.ENABLED);
                boolean useJavaContext = getBooleanIfSetOrGetDefault(adminObject, USE_JAVA_CONTEXT.getName(), Defaults.USE_JAVA_CONTEXT);

                CommonAdminObject adminObjet = new CommonAdminObjectImpl(configProperties, className, jndiName, poolName,
                        enabled, useJavaContext);

                adminObjets.add(adminObjet);
            }
        }
        return adminObjets;
    }

    private Long getLongIfSetOrGetDefault(ModelNode dataSourceNode, String key, Long defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asLong();
        } else {
            return defaultValue;
        }
    }

    private Integer getIntIfSetOrGetDefault(ModelNode dataSourceNode, String key, Integer defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asInt();
        } else {
            return defaultValue;
        }
    }

    private Boolean getBooleanIfSetOrGetDefault(ModelNode dataSourceNode, String key, Boolean defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asBoolean();
        } else {
            return defaultValue;
        }
    }

    private String getStringIfSetOrGetDefault(ModelNode dataSourceNode, String key, String defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asString();
        } else {
            return defaultValue;
        }
    }

    private Extension extractExtension(final ModelNode node, final String className, final String propertyName)
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

}
