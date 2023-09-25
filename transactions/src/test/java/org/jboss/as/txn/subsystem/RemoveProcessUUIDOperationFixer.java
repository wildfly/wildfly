/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import java.io.Serializable;

import org.jboss.as.model.test.OperationFixer;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RemoveProcessUUIDOperationFixer implements OperationFixer, Serializable {
    private static final long serialVersionUID = 1L;
    static final transient RemoveProcessUUIDOperationFixer INSTANCE = new RemoveProcessUUIDOperationFixer();

    private RemoveProcessUUIDOperationFixer(){
    }

    @Override
    public ModelNode fixOperation(ModelNode operation) {
        if (operation.hasDefined("process-id-uuid") && operation.get("process-id-uuid").asString() .equals("false")){
            operation.remove("process-id-uuid");
        }
        return operation;
    }
}
