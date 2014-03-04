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

package org.jboss.as.txn.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.txn.TransactionLogger.ROOT_LOGGER;
import static org.jboss.as.txn.TransactionMessages.MESSAGES;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * the {@link AbstractAddStepHandler} implementations that add CM managed resource.
 *
 * @author Stefano Maestri (c) 2011 Red Hat Inc.
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
            throw MESSAGES.jndiNameInvalidFormat();
        }

        CMResourceResourceDefinition.CM_TABLE_NAME.validateAndSet(operation, model);
        CMResourceResourceDefinition.CM_TABLE_BATCH_SIZE.validateAndSet(operation, model);
        CMResourceResourceDefinition.CM_TABLE_IMMEDIATE_CLEANUP.validateAndSet(operation, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String jndiName = address.getLastElement().getValue();
        final String tableName = CMResourceResourceDefinition.CM_TABLE_NAME.resolveModelAttribute(context, model).asString();
        final int batchSize =  CMResourceResourceDefinition.CM_TABLE_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        final boolean immediateCleanup = CMResourceResourceDefinition.CM_TABLE_IMMEDIATE_CLEANUP.resolveModelAttribute(context, model).asBoolean();
        ROOT_LOGGER.debugf("adding commit-markable-resource: jndi-name=%s, table-name=%s, batch-size=%d, immediate-cleanup=%b", jndiName, tableName, batchSize, immediateCleanup);

        JTAEnvironmentBean jTAEnvironmentBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);

        List<String> connectableResourceJNDINames = jTAEnvironmentBean.getCommitMarkableResourceJNDINames();
        Map<String, String> connectableResourceTableNameMap = jTAEnvironmentBean.getCommitMarkableResourceTableNameMap();
        Map<String, Boolean> performImmediateCleanupOfConnectableResourceBranchesMap = jTAEnvironmentBean.getPerformImmediateCleanupOfCommitMarkableResourceBranchesMap();
        Map<String, Integer> connectableResourceRecordDeleteBatchSizeMap = jTAEnvironmentBean.getCommitMarkableResourceRecordDeleteBatchSizeMap();

        connectableResourceJNDINames.add(jndiName);
        connectableResourceTableNameMap.put(jndiName, tableName);
        performImmediateCleanupOfConnectableResourceBranchesMap.put(jndiName, immediateCleanup);
        connectableResourceRecordDeleteBatchSizeMap.put(jndiName, batchSize);

        jTAEnvironmentBean.setCommitMarkableResourceJNDINames(connectableResourceJNDINames);
        jTAEnvironmentBean.setCommitMarkableResourceTableNameMap(connectableResourceTableNameMap);
        jTAEnvironmentBean.setPerformImmediateCleanupOfCommitMarkableResourceBranchesMap(performImmediateCleanupOfConnectableResourceBranchesMap);
        jTAEnvironmentBean.setCommitMarkableResourceRecordDeleteBatchSizeMap(connectableResourceRecordDeleteBatchSizeMap);
    }
}
