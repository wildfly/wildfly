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
