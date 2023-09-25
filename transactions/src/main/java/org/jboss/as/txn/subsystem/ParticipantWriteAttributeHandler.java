/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * The participant is a runtime resource which is created by {@link LogStoreProbeHandler}
 * and loaded with data from Narayana object store.
 * There is no way in Narayana API and no reason from processing perspective
 * to directly write to the participant record. The participant can be managed
 * by operations like {@code :delete}, {@code :recover} but not with direct write access
 * to attributes.
 *
 * This handler can be deleted in future.
 */
@Deprecated
public class ParticipantWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    public ParticipantWriteAttributeHandler(final AttributeDefinition... definitions) {
        super(definitions);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandback) {
        ModelNode subModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        ModelNode onAttribute = subModel.get(LogStoreConstants.JMX_ON_ATTRIBUTE);
        String jmxName = onAttribute.asString();
        MBeanServer mbs = TransactionExtension.getMBeanServer(context);

        try {
            ObjectName on = new ObjectName(jmxName);

            //  Invoke operation
            mbs.invoke(on, "clearHeuristic", null, null);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode resolvedValue, Void handback) {
        // no-op
    }
}
