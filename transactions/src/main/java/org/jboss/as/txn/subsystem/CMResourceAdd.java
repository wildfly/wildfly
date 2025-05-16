/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.txn.logging.TransactionLogger.ROOT_LOGGER;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.txn.service.CMResourceService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;

/**
 * the {@link AbstractAddStepHandler} implementations that add CM managed resource.
 *
 * @author Stefano Maestri (c) 2011 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class CMResourceAdd extends AbstractAddStepHandler {
    static CMResourceAdd INSTANCE = new CMResourceAdd();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        String str = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        if (!str.startsWith("java:/") && !str.startsWith("java:jboss/")) {
            throw ROOT_LOGGER.jndiNameInvalidFormat();
        }


        CMResourceResourceDefinition.CM_TABLE_NAME.validateAndSet(operation, model);
        CMResourceResourceDefinition.CM_TABLE_BATCH_SIZE.validateAndSet(operation, model);
        CMResourceResourceDefinition.CM_TABLE_IMMEDIATE_CLEANUP.validateAndSet(operation, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        if (!context.isBooting()) {
            context.restartRequired();
            return;
        }
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String jndiName = address.getLastElement().getValue();
        final String tableName = CMResourceResourceDefinition.CM_TABLE_NAME.resolveModelAttribute(context, model).asString();
        final int batchSize =  CMResourceResourceDefinition.CM_TABLE_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        final boolean immediateCleanup = CMResourceResourceDefinition.CM_TABLE_IMMEDIATE_CLEANUP.resolveModelAttribute(context, model).asBoolean();
        ROOT_LOGGER.debugf("adding commit-markable-resource: jndi-name=%s, table-name=%s, batch-size=%d, immediate-cleanup=%b", jndiName, tableName, batchSize, immediateCleanup);

        final ServiceBuilder<?> sb = context.getCapabilityServiceTarget().addService();
        final Consumer<Class<Void>> serviceConsumer = sb.provides(TxnServices.JBOSS_TXN_CMR.append(jndiName));
        final Supplier<JTAEnvironmentBean> jtaEnvironmentBeanSupplier = sb.requires(TxnServices.JBOSS_TXN_JTA_ENVIRONMENT);
        final CMResourceService service = new CMResourceService(serviceConsumer, jtaEnvironmentBeanSupplier, jndiName, tableName, immediateCleanup, batchSize);
        sb.setInstance(service).install();
    }
}
