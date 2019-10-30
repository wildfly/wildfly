/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTABLE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENLISTMENT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SHARABLE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRACKING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_GROUP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_USER;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;

import java.util.Map;

import org.jboss.as.connector.metadata.api.resourceadapter.WorkManagerSecurity;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.Pool;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.Security;
import org.jboss.jca.common.api.metadata.common.TimeOut;
import org.jboss.jca.common.api.metadata.common.Validation;
import org.jboss.jca.common.api.metadata.common.XaPool;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.AdminObject;
import org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;

/**
 * Handler for exposing transaction logs
 *
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a> (c) 2011 Red Hat Inc.
 * @author <a href="mmusgrove@redhat.com">Mike Musgrove</a> (c) 2012 Red Hat Inc.
 */
public class IronJacamarResourceCreator {

    public static final IronJacamarResourceCreator INSTANCE = new IronJacamarResourceCreator();


    private void setAttribute(ModelNode model, SimpleAttributeDefinition node, String value) {
        if (value != null) {
            model.get(node.getName()).set(value);
        }
    }

    private void setAttribute(ModelNode model, SimpleAttributeDefinition node, Boolean value) {
        if (value != null) {
            model.get(node.getName()).set(value);
        }
    }

    private void setAttribute(ModelNode model, SimpleAttributeDefinition node, Integer value) {
        if (value != null) {
            model.get(node.getName()).set(value);
        }
    }

    private void setAttribute(ModelNode model, SimpleAttributeDefinition node, Long value) {
        if (value != null) {
            model.get(node.getName()).set(value);
        }
    }


    private void addConfigProperties(final Resource parent, String name, String value) {
        final Resource config = new IronJacamarResource.IronJacamarRuntimeResource();
        final ModelNode model = config.getModel();
        model.get(Constants.CONFIG_PROPERTY_VALUE.getName()).set(value);
        final PathElement element = PathElement.pathElement(Constants.CONFIG_PROPERTIES.getName(), name);
        parent.registerChild(element, config);

    }




    private void addConnectionDefinition(final Resource parent, ConnectionDefinition connDef) {
        final Resource connDefResource = new IronJacamarResource.IronJacamarRuntimeResource();
        final ModelNode model = connDefResource.getModel();
        setAttribute(model, Constants.JNDINAME, connDef.getJndiName());
        if (connDef.getConfigProperties() != null) {
            for (Map.Entry<String, String> config : connDef.getConfigProperties().entrySet()) {
                addConfigProperties(connDefResource, config.getKey(), config.getValue());
            }
        }
        setAttribute(model, CLASS_NAME, connDef.getClassName());
        setAttribute(model, JNDINAME, connDef.getJndiName());
        setAttribute(model, USE_JAVA_CONTEXT, connDef.isUseJavaContext());
        setAttribute(model, ENABLED, connDef.isEnabled());

        setAttribute(model, CONNECTABLE, connDef.isConnectable());
        if (connDef.isTracking() != null) {
            setAttribute(model, TRACKING, connDef.isTracking());
        }

        setAttribute(model, USE_CCM, connDef.isUseCcm());
        setAttribute(model, SHARABLE, connDef.isSharable());
        setAttribute(model, ENLISTMENT, connDef.isEnlistment());

        final Pool pool = connDef.getPool();
        if (pool != null) {
            setAttribute(model, MAX_POOL_SIZE, pool.getMaxPoolSize());

            setAttribute(model, MIN_POOL_SIZE, pool.getMinPoolSize());


            setAttribute(model, INITIAL_POOL_SIZE, pool.getInitialPoolSize());
            if (pool.getCapacity() != null) {
                if (pool.getCapacity().getIncrementer() != null) {
                    setAttribute(model, CAPACITY_INCREMENTER_CLASS, pool.getCapacity().getIncrementer().getClassName());
                    if (pool.getCapacity().getIncrementer().getConfigPropertiesMap() != null) {
                        for (Map.Entry<String, String> config : pool.getCapacity().getIncrementer().getConfigPropertiesMap().entrySet()) {
                            model.get(CAPACITY_INCREMENTER_PROPERTIES.getName(), config.getKey()).set(config.getValue());
                        }
                    }
                }
                if (pool.getCapacity().getDecrementer() != null) {
                    setAttribute(model, CAPACITY_DECREMENTER_CLASS, pool.getCapacity().getDecrementer().getClassName());
                    if (pool.getCapacity().getDecrementer().getConfigPropertiesMap() != null) {
                        for (Map.Entry<String, String> config : pool.getCapacity().getDecrementer().getConfigPropertiesMap().entrySet()) {
                            model.get(CAPACITY_DECREMENTER_PROPERTIES.getName(), config.getKey()).set(config.getValue());
                        }
                    }

                }
            }


            setAttribute(model, POOL_USE_STRICT_MIN, pool.isUseStrictMin());

            if (pool.getFlushStrategy() != null)
                setAttribute(model, POOL_FLUSH_STRATEGY, pool.getFlushStrategy().name());
            setAttribute(model, POOL_PREFILL, pool.isPrefill());

            setAttribute(model, POOL_FAIR, pool.isFair());

            if (connDef.isXa()) {
                assert connDef.getPool() instanceof XaPool;
                XaPool xaPool = (XaPool) connDef.getPool();
                setAttribute(model, WRAP_XA_RESOURCE, xaPool.isWrapXaResource());
                setAttribute(model, SAME_RM_OVERRIDE, xaPool.isSameRmOverride());
                setAttribute(model, PAD_XID, xaPool.isPadXid());
                setAttribute(model, INTERLEAVING, xaPool.isInterleaving());
                setAttribute(model, NOTXSEPARATEPOOL, xaPool.isNoTxSeparatePool());
            }
        }
        final Security security = connDef.getSecurity();
        if (security != null) {
            setAttribute(model, APPLICATION, security.isApplication());

            if (security instanceof org.jboss.as.connector.metadata.api.common.Security &&
                    ((org.jboss.as.connector.metadata.api.common.Security) security).isElytronEnabled()) {
                setAttribute(model, ELYTRON_ENABLED, true);
                setAttribute(model, AUTHENTICATION_CONTEXT, security.getSecurityDomain());
                setAttribute(model, AUTHENTICATION_CONTEXT_AND_APPLICATION, security.getSecurityDomainAndApplication());
            } else {
                setAttribute(model, SECURITY_DOMAIN, security.getSecurityDomain());
                setAttribute(model, SECURITY_DOMAIN_AND_APPLICATION, security.getSecurityDomainAndApplication());
            }
        }
        final TimeOut timeOut = connDef.getTimeOut();
        if (timeOut != null) {
            setAttribute(model, ALLOCATION_RETRY, timeOut.getAllocationRetry());

            setAttribute(model, ALLOCATION_RETRY_WAIT_MILLIS, timeOut.getAllocationRetryWaitMillis());

            setAttribute(model, BLOCKING_TIMEOUT_WAIT_MILLIS, timeOut.getBlockingTimeoutMillis());

            setAttribute(model, IDLETIMEOUTMINUTES, timeOut.getIdleTimeoutMinutes());

            setAttribute(model, XA_RESOURCE_TIMEOUT, timeOut.getXaResourceTimeout());
        }
        final Validation validation = connDef.getValidation();
        if (validation != null) {
            setAttribute(model, BACKGROUNDVALIDATIONMILLIS, validation.getBackgroundValidationMillis());

            setAttribute(model, BACKGROUNDVALIDATION, validation.isBackgroundValidation());

            setAttribute(model, USE_FAST_FAIL, validation.isUseFastFail());

            setAttribute(model, VALIDATE_ON_MATCH, validation.isValidateOnMatch());
        }
        final Recovery recovery = connDef.getRecovery();
        if (recovery != null) {
            setAttribute(model, NO_RECOVERY, recovery.getNoRecovery());
            final Extension recoverPlugin = recovery.getRecoverPlugin();
            if (recoverPlugin != null) {
                setAttribute(model, RECOVERLUGIN_CLASSNAME, recoverPlugin.getClassName());
                if (recoverPlugin.getConfigPropertiesMap() != null) {
                    for (Map.Entry<String, String> config : recoverPlugin.getConfigPropertiesMap().entrySet()) {
                        model.get(RECOVERLUGIN_PROPERTIES.getName(), config.getKey()).set(config.getValue());
                    }
                }
            }
            final Credential recoveryCredential =  recovery.getCredential();
            if (recoveryCredential != null) {
                setAttribute(model, RECOVERY_PASSWORD, recoveryCredential.getPassword());
                if (recoveryCredential instanceof org.jboss.as.connector.metadata.api.common.Credential &&
                        ((org.jboss.as.connector.metadata.api.common.Credential) recoveryCredential).isElytronEnabled()) {
                    setAttribute(model, RECOVERY_ELYTRON_ENABLED, true);
                    setAttribute(model, RECOVERY_AUTHENTICATION_CONTEXT, recoveryCredential.getSecurityDomain());
                } else {
                    setAttribute(model, RECOVERY_SECURITY_DOMAIN, recoveryCredential.getSecurityDomain());
                }
                setAttribute(model, RECOVERY_USERNAME, recoveryCredential.getUserName());
            }
        }

        final Resource statsResource = new IronJacamarResource.IronJacamarRuntimeResource();
        connDefResource.registerChild( PathElement.pathElement(Constants.STATISTICS_NAME, "local"), statsResource);
        final PathElement element = PathElement.pathElement(Constants.CONNECTIONDEFINITIONS_NAME, connDef.getJndiName());
        parent.registerChild(element, connDefResource);

    }

    private void addAdminObject(final Resource parent, AdminObject adminObject) {
        final Resource adminObjectResource = new IronJacamarResource.IronJacamarRuntimeResource();
        final ModelNode model = adminObjectResource.getModel();
        setAttribute(model, CLASS_NAME, adminObject.getClassName());
        setAttribute(model, JNDINAME, adminObject.getJndiName());
        setAttribute(model, USE_JAVA_CONTEXT, adminObject.isUseJavaContext());
        setAttribute(model, ENABLED, adminObject.isEnabled());
        if (adminObject.getConfigProperties() != null) {
            for (Map.Entry<String, String> config : adminObject.getConfigProperties().entrySet()) {
                addConfigProperties(adminObjectResource, config.getKey(), config.getValue());
            }
        }
        final PathElement element = PathElement.pathElement(Constants.ADMIN_OBJECTS_NAME, adminObject.getJndiName());
        parent.registerChild(element, adminObjectResource);

    }


    private void addResourceAdapter(final Resource parent, String name, Activation ironJacamarMetadata) {
        final Resource ijResourceAdapter = new IronJacamarResource.IronJacamarRuntimeResource();
        final ModelNode model = ijResourceAdapter.getModel();
        model.get(Constants.ARCHIVE.getName()).set(name);
        setAttribute(model, Constants.BOOTSTRAP_CONTEXT, ironJacamarMetadata.getBootstrapContext());
        if (ironJacamarMetadata.getTransactionSupport() != null)
            model.get(Constants.TRANSACTION_SUPPORT.getName()).set(ironJacamarMetadata.getTransactionSupport().name());
        if (ironJacamarMetadata.getWorkManager() != null && ironJacamarMetadata.getWorkManager().getSecurity() != null) {
            org.jboss.jca.common.api.metadata.resourceadapter.WorkManagerSecurity security = ironJacamarMetadata.getWorkManager().getSecurity();
            model.get(Constants.WM_SECURITY.getName()).set(true);
            if (security.getDefaultGroups() != null) {
                for (String group : security.getDefaultGroups()) {
                    model.get(Constants.WM_SECURITY_DEFAULT_GROUPS.getName()).add(group);
                }
            }
            if (security.getDefaultPrincipal() != null)
                model.get(Constants.WM_SECURITY_DEFAULT_PRINCIPAL.getName()).set(security.getDefaultPrincipal());
            model.get(Constants.WM_SECURITY_MAPPING_REQUIRED.getName()).set(security.isMappingRequired());
            if (security instanceof  WorkManagerSecurity && ((WorkManagerSecurity) security).isElytronEnabled()) {
                model.get(Constants.WM_ELYTRON_SECURITY_DOMAIN.getName()).set(security.getDomain());
            }
            else {
                model.get(Constants.WM_SECURITY_DOMAIN.getName()).set(security.getDomain());
            }
            if (security.getGroupMappings() != null) {
                for (Map.Entry<String, String> entry : security.getGroupMappings().entrySet()) {
                    final Resource mapping = new IronJacamarResource.IronJacamarRuntimeResource();
                    final ModelNode subModel = mapping.getModel();
                    subModel.get(Constants.WM_SECURITY_MAPPING_FROM.getName()).set(entry.getKey());
                    subModel.get(Constants.WM_SECURITY_MAPPING_TO.getName()).set(entry.getKey());
                    final PathElement element = PathElement.pathElement(Constants.WM_SECURITY_MAPPING_GROUPS.getName(), WM_SECURITY_MAPPING_GROUP.getName());
                    ijResourceAdapter.registerChild(element, mapping);
                }
            }
            if (security.getUserMappings() != null) {
                for (Map.Entry<String, String> entry : security.getUserMappings().entrySet()) {
                    final Resource mapping = new IronJacamarResource.IronJacamarRuntimeResource();
                    final ModelNode subModel = mapping.getModel();
                    subModel.get(Constants.WM_SECURITY_MAPPING_FROM.getName()).set(entry.getKey());
                    subModel.get(Constants.WM_SECURITY_MAPPING_TO.getName()).set(entry.getKey());
                    final PathElement element = PathElement.pathElement(Constants.WM_SECURITY_MAPPING_USERS.getName(), WM_SECURITY_MAPPING_USER.getName());
                    ijResourceAdapter.registerChild(element, mapping);
                }
            }
        }
        if (ironJacamarMetadata.getBeanValidationGroups() != null) {
            for (String bv : ironJacamarMetadata.getBeanValidationGroups()) {
                model.get(Constants.BEANVALIDATION_GROUPS.getName()).add(new ModelNode().set(bv));
            }
        }
        if (ironJacamarMetadata.getConfigProperties() != null) {
            for (Map.Entry<String, String> config : ironJacamarMetadata.getConfigProperties().entrySet()) {
                addConfigProperties(ijResourceAdapter, config.getKey(), config.getValue());
            }
        }
        if (ironJacamarMetadata.getConnectionDefinitions() != null) {
            for (ConnectionDefinition connDef : ironJacamarMetadata.getConnectionDefinitions()) {
                addConnectionDefinition(ijResourceAdapter, connDef);
            }
        }
        if (ironJacamarMetadata.getAdminObjects() != null) {
            for (AdminObject adminObject : ironJacamarMetadata.getAdminObjects()) {
                addAdminObject(ijResourceAdapter, adminObject);
            }
        }
        final Resource statsResource = new IronJacamarResource.IronJacamarRuntimeResource();
        ijResourceAdapter.registerChild( PathElement.pathElement(Constants.STATISTICS_NAME, "local"), statsResource);

        final PathElement element = PathElement.pathElement(Constants.RESOURCEADAPTER_NAME, name);
        parent.registerChild(element, ijResourceAdapter);


    }

    private Resource getIronJacamarResource(AS7MetadataRepository mdr, String name) {

        final Resource resource = Resource.Factory.create();

        Activation activation = mdr.getIronJacamarMetaData(name);
        if (activation != null)
            addResourceAdapter(resource, name, activation);

        return resource;


    }

    public void execute(Resource parentResource, AS7MetadataRepository mdr, String name) {


        // Get the iron-jacamar resource
        final IronJacamarResource ironJacamarResource = new IronJacamarResource();
        // Replace the current model with an updated one
        final Resource storeModel = getIronJacamarResource(mdr, name);

        ironJacamarResource.update(storeModel);
        PathElement ijPe = PathElement.pathElement(Constants.IRONJACAMAR_NAME, Constants.IRONJACAMAR_NAME);
        if (parentResource.getChild(ijPe) == null) {
            parentResource.registerChild(ijPe, ironJacamarResource);
        }
    }

    private void setStatsModelValue(ModelNode result, String attributeName, StatisticsPlugin stats) {
        if (stats.getType(attributeName) == int.class) {
            result.set((Integer) stats.getValue(attributeName));
        } else if (stats.getType(attributeName) == long.class) {
            result.set((Long) stats.getValue(attributeName));
        } else {
            result.set("" + stats.getValue(attributeName));
        }
    }

}
