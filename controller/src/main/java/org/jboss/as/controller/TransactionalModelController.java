/**
 *
 */
package org.jboss.as.controller;

import org.jboss.as.controller.client.ExecutionContext;


/**
 * TODO add class javadoc for TransactionalModelController
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public interface TransactionalModelController extends ModelController {

    OperationResult execute(ExecutionContext executionContext, ResultHandler handler, ControllerTransactionContext transaction);
}
