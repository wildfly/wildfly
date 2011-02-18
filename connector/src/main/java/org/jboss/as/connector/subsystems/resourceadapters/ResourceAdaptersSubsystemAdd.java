/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAPCONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOLNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTIONSUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.processors.ResourceAdaptersAttachingProcessor;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.metadata.common.CommonAdminObjectImpl;
import org.jboss.jca.common.metadata.common.CommonConnDefImpl;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonSecurityImpl;
import org.jboss.jca.common.metadata.common.CommonTimeOutImpl;
import org.jboss.jca.common.metadata.common.CommonValidationImpl;
import org.jboss.jca.common.metadata.resourceadapter.ResourceAdapterImpl;
import org.jboss.jca.common.metadata.resourceadapter.ResourceAdaptersImpl;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class ResourceAdaptersSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final ResourceAdaptersSubsystemAdd INSTANCE = new ResourceAdaptersSubsystemAdd();

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        // Populate subModel
        final ModelNode subModel = context.getSubModel();
        subModel.setEmptyObject();

        // Workaround to populate domain model.

        boolean workaround = true;

        if (workaround) {
            if (operation.has(RESOURCEADAPTERS)) {
                ModelNode datasources = operation.get(RESOURCEADAPTERS);
                subModel.get(RESOURCEADAPTERS).set(datasources);
            }
        } else {
            if (operation.hasDefined(RESOURCEADAPTERS)) {
                for (ModelNode raNode : operation.get(RESOURCEADAPTERS).asList()) {
                    for (ModelNode property : raNode.get(CONFIG_PROPERTIES).asList()) {
                        subModel.get(CONFIG_PROPERTIES, property.asProperty().getName()).set(property.asString());
                    }
                    for (final String attribute : ResourceAdaptersSubsystemProviders.RESOURCEADAPTER_ATTRIBUTE) {
                        if (raNode.get(attribute).isDefined()) {
                            subModel.get(attribute).set(raNode.get(attribute));
                        }
                    }
                }
            }
        }

        if (context instanceof BootOperationContext) {
            final BootOperationContext updateContext = (BootOperationContext) context;
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();

                    ResourceAdapters resourceAdapters = buildResourceAdaptersObject(operation);
                    serviceTarget.addService(ConnectorServices.RESOURCEADAPTERS_SERVICE, new ResourceAdaptersService(resourceAdapters))
                            .setInitialMode(Mode.ACTIVE).install();

                    updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_RESOURCE_ADAPTERS, new ResourceAdaptersAttachingProcessor(resourceAdapters));
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        return new BasicOperationResult(compensatingOperation);
    }

    private ResourceAdapters buildResourceAdaptersObject(ModelNode operation) {
        List<ResourceAdapter> resourceAdapters = new ArrayList<ResourceAdapter>();

        if (operation.hasDefined(RESOURCEADAPTERS)) {
            for (ModelNode raNode : operation.get(RESOURCEADAPTERS).asList()) {
                Map<String, String> configProperties = new HashMap<String, String>(raNode.get(CONFIG_PROPERTIES).asList()
                        .size());
                for (ModelNode property : raNode.get(CONFIG_PROPERTIES).asList()) {
                    configProperties.put(property.asProperty().getName(), property.asString());
                }
                String archive = getStringIfSetOrGetDefault(operation, ARCHIVE, null);
                TransactionSupportEnum transactionSupport = operation.hasDefined(TRANSACTIONSUPPORT) ? TransactionSupportEnum
                        .valueOf(operation.get(TRANSACTIONSUPPORT).asString()) : null;
                String bootstrapContext = getStringIfSetOrGetDefault(operation, BOOTSTRAPCONTEXT, null);
                List<String> beanValidationGroups = new ArrayList<String>(operation.get(BEANVALIDATIONGROUPS).asList().size());
                for (ModelNode beanValidtion : operation.get(BEANVALIDATIONGROUPS).asList()) {
                    beanValidationGroups.add(beanValidtion.asString());
                }

                ResourceAdapter ra = new ResourceAdapterImpl(archive, transactionSupport,
                        buildConnectionDefinitionObject(operation), buildAdminObjects(operation), configProperties,
                        beanValidationGroups, bootstrapContext);

                resourceAdapters.add(ra);
            }
        }

        return new ResourceAdaptersImpl(resourceAdapters);

    }

    private List<CommonConnDef> buildConnectionDefinitionObject(ModelNode parentNode) {
        List<CommonConnDef> connDefs = new ArrayList<CommonConnDef>();

        for (ModelNode conDefNode : parentNode.get(CONNECTIONDEFINITIONS).asList()) {
            Map<String, String> configProperties = new HashMap<String, String>(conDefNode.get(CONFIG_PROPERTIES).asList()
                    .size());
            for (ModelNode property : conDefNode.get(CONFIG_PROPERTIES).asList()) {
                configProperties.put(property.asProperty().getName(), property.asString());
            }
            String className = getStringIfSetOrGetDefault(conDefNode, CLASS_NAME, null);
            String jndiName = getStringIfSetOrGetDefault(conDefNode, JNDI_NAME, null);
            String poolName = getStringIfSetOrGetDefault(conDefNode, POOLNAME, null);
            boolean enabled = getBooleanIfSetOrGetDefault(conDefNode, ENABLED, false);
            boolean useJavaContext = getBooleanIfSetOrGetDefault(conDefNode, USE_JAVA_CONTEXT, false);

            Integer maxPoolSize = getIntIfSetOrGetDefault(conDefNode, MAX_POOL_SIZE, null);
            Integer minPoolSize = getIntIfSetOrGetDefault(conDefNode, MIN_POOL_SIZE, null);
            boolean prefill = getBooleanIfSetOrGetDefault(conDefNode, POOL_PREFILL, false);
            boolean useStrictMin = getBooleanIfSetOrGetDefault(conDefNode, POOL_USE_STRICT_MIN, false);

            Integer allocationRetry = getIntIfSetOrGetDefault(conDefNode, ALLOCATION_RETRY, null);
            Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(conDefNode, ALLOCATION_RETRY_WAIT_MILLIS, null);
            Long blockingTimeoutMillis = getLongIfSetOrGetDefault(conDefNode, BLOCKING_TIMEOUT_WAIT_MILLIS, null);
            Long idleTimeoutMinutes = getLongIfSetOrGetDefault(conDefNode, IDLETIMEOUTMINUTES, null);
            Integer xaResourceTimeout = getIntIfSetOrGetDefault(conDefNode, XA_RESOURCE_TIMEOUT, null);
            CommonTimeOut timeOut = new CommonTimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                    allocationRetryWaitMillis, xaResourceTimeout);
            CommonPool pool = new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin);

            String username = getStringIfSetOrGetDefault(conDefNode, USERNAME, null);
            String password = getStringIfSetOrGetDefault(conDefNode, PASSWORD, null);
            CommonSecurity security = new CommonSecurityImpl(username, password);

            Long backgroundValidationMinutes = getLongIfSetOrGetDefault(conDefNode, BACKGROUNDVALIDATIONMINUTES, null);
            boolean backgroundValidation = getBooleanIfSetOrGetDefault(conDefNode, BACKGROUNDVALIDATION, false);
            boolean useFastFail = getBooleanIfSetOrGetDefault(conDefNode, USE_FAST_FAIL, false);
            CommonValidation validation = new CommonValidationImpl(backgroundValidation, backgroundValidationMinutes,
                    useFastFail);

            CommonConnDef connectionDefinition = new CommonConnDefImpl(configProperties, className, jndiName, poolName,
                    enabled, useJavaContext, pool, timeOut, validation, security);

            connDefs.add(connectionDefinition);
        }
        return connDefs;
    }

    private List<CommonAdminObject> buildAdminObjects(ModelNode parentNode) {
        List<CommonAdminObject> adminObjets = new ArrayList<CommonAdminObject>();

        for (ModelNode adminObject : parentNode.get(ADMIN_OBJECTS).asList()) {
            Map<String, String> configProperties = new HashMap<String, String>(adminObject.get(CONFIG_PROPERTIES).asList()
                    .size());
            for (ModelNode property : adminObject.get(CONFIG_PROPERTIES).asList()) {
                configProperties.put(property.asProperty().getName(), property.asString());
            }
            String className = getStringIfSetOrGetDefault(adminObject, CLASS_NAME, null);
            String jndiName = getStringIfSetOrGetDefault(adminObject, JNDI_NAME, null);
            String poolName = getStringIfSetOrGetDefault(adminObject, POOLNAME, null);
            boolean enabled = getBooleanIfSetOrGetDefault(adminObject, ENABLED, false);
            boolean useJavaContext = getBooleanIfSetOrGetDefault(adminObject, USE_JAVA_CONTEXT, false);

            CommonAdminObject adminObjet = new CommonAdminObjectImpl(configProperties, className, jndiName, poolName, enabled,
                    useJavaContext);

            adminObjets.add(adminObjet);
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

    private boolean getBooleanIfSetOrGetDefault(ModelNode dataSourceNode, String key, boolean defaultValue) {
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
}
