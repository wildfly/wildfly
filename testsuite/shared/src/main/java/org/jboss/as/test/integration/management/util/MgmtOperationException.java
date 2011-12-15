/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.management.util;

import org.jboss.dmr.ModelNode;

/**
 *
 * @author dpospisi
 */
public class MgmtOperationException extends Exception {

    private ModelNode operation;
    private ModelNode result;
    
    /**
     * Creates a new instance of
     * <code>MgmtOperationException</code> without detail message.
     */
    public MgmtOperationException() {
    }

    /**
     * Constructs an instance of
     * <code>MgmtOperationException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public MgmtOperationException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of
     * <code>MgmtOperationException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public MgmtOperationException(String msg, ModelNode operation, ModelNode result) {
        super(msg);
        this.operation = operation;
        this.result = result;
    }

    /**
     * @return the operation
     */
    public ModelNode getOperation() {
        return operation;
    }

    /**
     * @return the result
     */
    public ModelNode getResult() {
        return result;
    }
    
}
