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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class LogStoreTransactionDeleteHandler implements OperationStepHandler {

    static final LogStoreTransactionDeleteHandler INSTANCE = new LogStoreTransactionDeleteHandler();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        MBeanServer mbs = TransactionExtension.getMBeanServer(context);
        ModelNode subModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        ModelNode onAttribute = subModel.get(LogStoreConstants.JMX_ON_ATTRIBUTE);
        String jmxName = onAttribute.asString();

        try {
            ObjectName on = new ObjectName(jmxName);

            //  Invoke operation
            mbs.invoke(on, "remove", null, null);

            try {
                // validate that the MBean was removed:
                mbs.getObjectInstance( new ObjectName(jmxName));
            } catch (InstanceNotFoundException e) {
                // success, the MBean was deleted
                context.removeResource(PathAddress.EMPTY_ADDRESS);
                context.completeStep();
            } catch (MalformedObjectNameException e) {
                throw new OperationFailedException(e.getMessage());
            }

        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage());
        }


    }
}
