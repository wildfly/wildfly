/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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


package org.jboss.as.test.integration.transactions;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Utility class for management operations
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public final class ManagementUtil {
    private static final Logger log = Logger.getLogger(ManagementUtil.class);

    public static ModelNode readAttribute(ModelNode address, String name, ManagementClient mgmtClient) throws IOException, MgmtOperationException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(INCLUDE_DEFAULTS).set("true");
        operation.get(NAME).set(name);
        return executeOperation(operation, mgmtClient);
    }

    /**
     * @param op  op to execute
     * @param isUnwrapResult true == just RESULT part of outcome will be returned, false == whole outcome (wrapped one) is returned
     */
    public static ModelNode executeOperation(final ModelNode op, boolean isUnwrapResult, ManagementClient mgmtClient) throws IOException, MgmtOperationException {
        return executeOperation(op, isUnwrapResult, mgmtClient.getControllerClient());
    }

    /**
     * @param op  op to execute, just RESULT part of outcome (put in model node ) will be returned
     */
    public static ModelNode executeOperation(final ModelNode op, ManagementClient mgmtClient) throws IOException, MgmtOperationException {
        return executeOperation(op, true, mgmtClient);
    }

    /**
     * Executing operation - check the parameters.
     *
     * @param op  operation model node which will be executed
     * @param isUnwrapResult  boolean attribute defines whether the result shoud be unwrapped or not
     *      when it is unwrapped (param == true) then just result part will be returned
     *   when it is wrapped (param == false)  then the whole outcome will be returned
     * @param clientToExecute  what client use for execution
     * @return model node - unwrapped or wrapped
     */
    public static ModelNode executeOperation(final ModelNode op, boolean isUnwrapResult, ModelControllerClient clientToExecute)
            throws IOException, MgmtOperationException {

        // operation execution
        ModelNode ret = clientToExecute.execute(op);
        if (!isUnwrapResult) return ret;  // do not unwrap the result - return all outcome to me

        if(SUCCESS.equals(ret.get(OUTCOME).asString())) {
            log.info("Succesful management operation " + op + " with result " + ret);
        }

        if (!SUCCESS.equals(ret.get(OUTCOME).asString())) {
            log.errorf("Management operation %s failed: %s", op, ret);
            throw new MgmtOperationException("Management operation failed: " + ret.get(FAILURE_DESCRIPTION), op, ret);
        }

        return ret.get(RESULT);
    }
}
